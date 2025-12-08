#!/bin/bash

# Script to transform all PDF content items to text renditions
# Usage: ./transform_pdfs.sh [base_url]
#
# This script queries all content items, finds PDFs, and creates text renditions
# for them using the transformation API.

BASE_URL="${1:-http://localhost:8082/docmgmt}"
API_URL="${BASE_URL}/api"

echo "==================================================================="
echo "PDF to Text Transformation Script"
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

# Get all content items
echo "Fetching all content items..."
CONTENT_RESPONSE=$(curl -s "${API_URL}/content/by-sysobject/1" 2>/dev/null)

if [ $? -ne 0 ]; then
    echo "WARNING: Could not fetch content. Trying alternative approach..."
    echo "You'll need to manually identify PDF content IDs."
    echo ""
    echo "Usage:"
    echo "  curl -X POST ${API_URL}/content/{content_id}/transform"
    echo ""
    exit 1
fi

# Count total PDFs and transformations
TOTAL_PDFS=0
SUCCESSFUL=0
FAILED=0
SKIPPED=0

echo "Processing content items..."
echo ""

# Parse JSON and find PDF content items
# Note: This requires jq. If not available, you'll need to manually identify PDF content IDs.
if ! command -v jq &> /dev/null; then
    echo "WARNING: 'jq' is not installed. Cannot automatically process content."
    echo ""
    echo "To transform a PDF manually:"
    echo "  1. Find PDF content IDs from the UI or API"
    echo "  2. Run: curl -X POST ${API_URL}/content/{content_id}/transform"
    echo ""
    exit 1
fi

# Get all documents first
echo "Fetching all documents..."
DOCS=$(curl -s "${API_URL}/documents")

if [ -z "$DOCS" ] || [ "$DOCS" == "null" ]; then
    echo "No documents found or error fetching documents."
    exit 1
fi

DOC_IDS=$(echo "$DOCS" | jq -r '.[].id')

for DOC_ID in $DOC_IDS; do
    echo "Processing document ID: ${DOC_ID}"
    
    # Get content for this document
    CONTENT_LIST=$(curl -s "${API_URL}/content/by-sysobject/${DOC_ID}")
    
    if [ -z "$CONTENT_LIST" ] || [ "$CONTENT_LIST" == "null" ] || [ "$CONTENT_LIST" == "[]" ]; then
        continue
    fi
    
    # Process each content item
    echo "$CONTENT_LIST" | jq -c '.[]' | while read -r CONTENT; do
        CONTENT_ID=$(echo "$CONTENT" | jq -r '.id')
        CONTENT_TYPE=$(echo "$CONTENT" | jq -r '.contentType')
        CONTENT_NAME=$(echo "$CONTENT" | jq -r '.name')
        IS_PRIMARY=$(echo "$CONTENT" | jq -r '.isPrimary // true')
        
        # Check if it's a PDF and primary
        if [[ "$CONTENT_TYPE" == "application/pdf" ]] && [[ "$IS_PRIMARY" == "true" ]]; then
            TOTAL_PDFS=$((TOTAL_PDFS + 1))
            echo "  Found PDF: ${CONTENT_NAME} (ID: ${CONTENT_ID})"
            
            # Check if text rendition already exists
            RENDITIONS=$(curl -s "${API_URL}/content/${CONTENT_ID}/renditions" 2>/dev/null)
            HAS_TEXT_RENDITION=$(echo "$RENDITIONS" | jq -r '[.[] | select(.contentType == "text/plain")] | length')
            
            if [ "$HAS_TEXT_RENDITION" -gt 0 ]; then
                echo "    ⊘ Text rendition already exists, skipping..."
                SKIPPED=$((SKIPPED + 1))
            else
                echo "    → Transforming to text..."
                TRANSFORM_RESPONSE=$(curl -s -X POST "${API_URL}/content/${CONTENT_ID}/transform" 2>/dev/null)
                
                if [ $? -eq 0 ]; then
                    # Check if response indicates success
                    ERROR_MSG=$(echo "$TRANSFORM_RESPONSE" | jq -r '.message // .error // empty' 2>/dev/null)
                    if [ -z "$ERROR_MSG" ]; then
                        echo "    ✓ Successfully created text rendition"
                        SUCCESSFUL=$((SUCCESSFUL + 1))
                    else
                        echo "    ✗ Failed: ${ERROR_MSG}"
                        FAILED=$((FAILED + 1))
                    fi
                else
                    echo "    ✗ Failed to transform"
                    FAILED=$((FAILED + 1))
                fi
            fi
        fi
    done
done

echo ""
echo "==================================================================="
echo "Transformation Summary"
echo "==================================================================="
echo "Total PDFs found:      ${TOTAL_PDFS}"
echo "Successfully transformed: ${SUCCESSFUL}"
echo "Already had renditions: ${SKIPPED}"
echo "Failed:                ${FAILED}"
echo "==================================================================="
