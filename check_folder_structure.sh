#!/bin/bash

# Script to diagnose folder structure issues after CLI import
# Usage: ./check_folder_structure.sh [base_url]

BASE_URL="${1:-http://localhost:8082/docmgmt}"
API_URL="${BASE_URL}/api"

echo "==================================================================="
echo "Folder Structure Diagnostic"
echo "==================================================================="
echo "API Base URL: ${API_URL}"
echo ""

# Check if server is running
echo "Checking server connectivity..."
if ! curl -s "${BASE_URL}/actuator/health" > /dev/null; then
    echo "ERROR: Cannot connect to server at ${BASE_URL}"
    echo "Make sure the server is running and the URL is correct."
    exit 1
fi
echo "✓ Server is running"
echo ""

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "WARNING: 'jq' is not installed. Install it for better output formatting."
    echo ""
    echo "  macOS:  brew install jq"
    echo "  Linux:  sudo apt-get install jq  (or yum install jq)"
    echo ""
    exit 1
fi

# Get all folders
echo "Fetching all folders..."
ALL_FOLDERS=$(curl -s "${API_URL}/folders")
FOLDER_COUNT=$(echo "$ALL_FOLDERS" | jq 'length')
echo "Total folders in system: ${FOLDER_COUNT}"
echo ""

# Get root folders
echo "Fetching root folders..."
ROOT_FOLDERS=$(curl -s "${API_URL}/folders/roots")
ROOT_COUNT=$(echo "$ROOT_FOLDERS" | jq 'length')
echo "Root folders (no parent): ${ROOT_COUNT}"
echo ""

if [ "$ROOT_COUNT" -gt 0 ]; then
    echo "Root folders:"
    echo "$ROOT_FOLDERS" | jq -r '.[] | "  - \(.name) (ID: \(.id)) - Path: \(.path) - Items: \(.items | length)"'
    echo ""
fi

# Get all documents
echo "Fetching all documents..."
ALL_DOCS=$(curl -s "${API_URL}/documents")
DOC_COUNT=$(echo "$ALL_DOCS" | jq 'length')
echo "Total documents in system: ${DOC_COUNT}"
echo ""

# Check which documents are in folders
echo "Checking document-folder relationships..."
DOCS_IN_FOLDERS=0
DOCS_NOT_IN_FOLDERS=0

echo "$ALL_DOCS" | jq -r '.[].id' | while read doc_id; do
    # Find folders containing this document
    FOLDER_LIST=$(echo "$ALL_FOLDERS" | jq --arg docid "$doc_id" '[.[] | select(.items != null and (.items | map(.id) | contains([($docid | tonumber)]))] | {id: .id, name: .name}')
    FOLDER_LIST_COUNT=$(echo "$FOLDER_LIST" | jq 'length')
    
    if [ "$FOLDER_LIST_COUNT" -gt 0 ]; then
        DOCS_IN_FOLDERS=$((DOCS_IN_FOLDERS + 1))
    else
        DOCS_NOT_IN_FOLDERS=$((DOCS_NOT_IN_FOLDERS + 1))
    fi
done

echo ""
echo "Document placement summary:"
echo "  Documents in folders:     ${DOCS_IN_FOLDERS}"
echo "  Documents not in folders: ${DOCS_NOT_IN_FOLDERS}"
echo ""

# Analyze folder structure depth
echo "Analyzing folder hierarchy..."
echo "$ALL_FOLDERS" | jq -r '.[] | select(.parentFolderId != null) | "Child folder: \(.name) (ID: \(.id)) -> Parent ID: \(.parentFolderId)"' | head -20
if [ $(echo "$ALL_FOLDERS" | jq '[.[] | select(.parentFolderId != null)] | length') -gt 20 ]; then
    echo "  ... (showing first 20 child folders)"
fi
echo ""

# Count orphaned documents (not in any folder)
echo "Checking for orphaned documents (not in any folder)..."
ORPHANED_DOCS=$(mktemp)
echo "$ALL_DOCS" | jq -r '.[].id' | while read doc_id; do
    DOC_NAME=$(echo "$ALL_DOCS" | jq -r --arg docid "$doc_id" '.[] | select(.id == ($docid | tonumber)) | .name')
    FOLDER_LIST=$(echo "$ALL_FOLDERS" | jq --arg docid "$doc_id" '[.[] | select(.items != null and (.items | map(.id) | contains([($docid | tonumber)])))]')
    FOLDER_COUNT=$(echo "$FOLDER_LIST" | jq 'length')
    
    if [ "$FOLDER_COUNT" -eq 0 ]; then
        echo "Document ID ${doc_id}: ${DOC_NAME}" >> "$ORPHANED_DOCS"
    fi
done

ORPHAN_COUNT=$(wc -l < "$ORPHANED_DOCS" | tr -d ' ')
if [ "$ORPHAN_COUNT" -gt 0 ]; then
    echo "Found ${ORPHAN_COUNT} orphaned documents:"
    head -20 "$ORPHANED_DOCS" | while read line; do
        echo "  - $line"
    done
    if [ "$ORPHAN_COUNT" -gt 20 ]; then
        echo "  ... (showing first 20)"
    fi
else
    echo "No orphaned documents found - all documents are in at least one folder."
fi
rm "$ORPHANED_DOCS"
echo ""

# Show sample folder structure
echo "Sample folder structure (first root folder and its contents):"
FIRST_ROOT_ID=$(echo "$ROOT_FOLDERS" | jq -r '.[0].id // empty')
if [ -n "$FIRST_ROOT_ID" ]; then
    FIRST_ROOT=$(echo "$ROOT_FOLDERS" | jq --arg id "$FIRST_ROOT_ID" '.[] | select(.id == ($id | tonumber))')
    echo "Root: $(echo "$FIRST_ROOT" | jq -r '.name') (ID: ${FIRST_ROOT_ID})"
    
    # Get children
    CHILDREN=$(curl -s "${API_URL}/folders/${FIRST_ROOT_ID}/children")
    CHILD_COUNT=$(echo "$CHILDREN" | jq 'length')
    if [ "$CHILD_COUNT" -gt 0 ]; then
        echo "  Children (${CHILD_COUNT}):"
        echo "$CHILDREN" | jq -r '.[] | "    - \(.name) (ID: \(.id)) - Items: \(.items | length)"' | head -10
        if [ "$CHILD_COUNT" -gt 10 ]; then
            echo "    ... (showing first 10)"
        fi
    else
        echo "  No child folders"
    fi
    
    # Get items in root folder
    ITEMS=$(echo "$FIRST_ROOT" | jq '.items')
    ITEM_COUNT=$(echo "$ITEMS" | jq 'length')
    echo "  Direct items (${ITEM_COUNT}):"
    if [ "$ITEM_COUNT" -gt 0 ]; then
        echo "$ITEMS" | jq -r '.[] | "    - \(.name) (ID: \(.id), Type: \(.type))"' | head -10
        if [ "$ITEM_COUNT" -gt 10 ]; then
            echo "    ... (showing first 10)"
        fi
    fi
else
    echo "No root folders found."
fi
echo ""

echo "==================================================================="
echo "Recommendations"
echo "==================================================================="

if [ "$ROOT_COUNT" -eq 0 ]; then
    echo "⚠️  No root folders found!"
    echo "    The CLI import should have created folders matching your directory structure."
    echo "    Check CLI logs for errors during folder creation."
fi

if [ "$ORPHAN_COUNT" -gt 0 ]; then
    echo "⚠️  ${ORPHAN_COUNT} documents are not in any folder!"
    echo "    This suggests the CLI's folder linking step failed."
    echo "    Possible causes:"
    echo "    - REST API endpoint not working"
    echo "    - Silent errors in CLI (no exception handling)"
    echo "    - Database transaction rollback"
fi

if [ "$FOLDER_COUNT" -gt 0 ] && [ "$CHILD_COUNT" -eq 0 ]; then
    echo "⚠️  Folders exist but have no parent-child relationships!"
    echo "    This suggests the folder hierarchy was flattened during import."
fi

if [ "$DOC_COUNT" -gt 0 ] && [ "$FOLDER_COUNT" -gt 0 ] && [ "$ORPHAN_COUNT" -eq 0 ]; then
    echo "✓ Folder structure looks good!"
    echo "  All documents are properly organized in folders."
fi

echo "==================================================================="
