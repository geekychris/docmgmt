#!/usr/bin/env python3
"""
Document Management CLI Tool

A command-line interface for performing CRUD operations against the Document Management API.
"""

import argparse
import json
import os
import sys
import tempfile
import subprocess
from typing import Dict, Any, Optional, List, Union
import requests

# Configuration
BASE_URL = "http://localhost:8082/docmgmt/api"
DEFAULT_CONTENT_TYPE = "text/markdown"
JSON_HEADERS = {"Content-Type": "application/json"}


def configure_parser() -> argparse.ArgumentParser:
    """Configure argument parser with all commands and options."""
    parser = argparse.ArgumentParser(
        description="Command line tool for Document Management API operations",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # List all filestores
  python docmgmt.py filestore list
  
  # Create a new filestore
  python docmgmt.py filestore create --name "project-docs" --description "Project documentation"
  
  # List documents in a filestore
  python docmgmt.py document list --filestore-id 123e4567-e89b-12d3-a456-426614174000
  
  # Create a document with content in one step
  python docmgmt.py document create-with-content \\
    --filestore-id 123e4567-e89b-12d3-a456-426614174000 \\
    --name "readme" \\
    --doc-type "ARTICLE" \\
    --content-file ./README.md \\
    --content-type "text/markdown"
    
  # Get document details
  python docmgmt.py document get --id 123e4567-e89b-12d3-a456-426614174000
  
  # Download content
  python docmgmt.py content download --id 123e4567-e89b-12d3-a456-426614174000 --output ./downloaded_content.md
"""
    )

    # Create subparsers for main categories
    subparsers = parser.add_subparsers(dest="entity", help="Entity to operate on")

    # Filestore commands
    filestore_parser = subparsers.add_parser("filestore", help="Operations on filestores")
    filestore_subparsers = filestore_parser.add_subparsers(dest="action", help="Action to perform on filestores")

    # Filestore - List
    filestore_list = filestore_subparsers.add_parser("list", help="List all filestores")
    filestore_list.add_argument("--name", help="Filter by filestore name")

    # Filestore - Create
    filestore_create = filestore_subparsers.add_parser("create", help="Create a new filestore")
    filestore_create.add_argument("--name", required=True, help="Filestore name")
    filestore_create.add_argument("--description", help="Filestore description")

    # Filestore - Get
    filestore_get = filestore_subparsers.add_parser("get", help="Get filestore details")
    filestore_get.add_argument("--id", required=True, help="Filestore ID")

    # Filestore - Delete
    filestore_delete = filestore_subparsers.add_parser("delete", help="Delete a filestore")
    filestore_delete.add_argument("--id", required=True, help="Filestore ID")

    # Document commands
    document_parser = subparsers.add_parser("document", help="Operations on documents")
    document_subparsers = document_parser.add_subparsers(dest="action", help="Action to perform on documents")

    # Document - List
    document_list = document_subparsers.add_parser("list", help="List documents")
    document_list.add_argument("--filestore-id", required=True, help="Filestore ID")
    document_list.add_argument("--name", help="Filter by document name")

    # Document - Create
    document_create = document_subparsers.add_parser("create", help="Create a new document")
    document_create.add_argument("--filestore-id", required=True, help="Filestore ID")
    document_create.add_argument("--name", required=True, help="Document name")
    document_create.add_argument("--doc-type", required=True, help="Document type (e.g., ARTICLE, REPORT)")
    document_create.add_argument("--metadata", help="JSON string with document metadata")

    # Document - Get
    document_get = document_subparsers.add_parser("get", help="Get document details")
    document_get.add_argument("--id", required=True, help="Document ID")

    # Document - Update
    document_update = document_subparsers.add_parser("update", help="Update a document")
    document_update.add_argument("--id", required=True, help="Document ID")
    document_update.add_argument("--name", help="New document name")
    document_update.add_argument("--doc-type", help="New document type")
    document_update.add_argument("--metadata", help="JSON string with updated metadata")

    # Document - Delete
    document_delete = document_subparsers.add_parser("delete", help="Delete a document")
    document_delete.add_argument("--id", required=True, help="Document ID")

    # Document - Create with content
    document_create_with_content = document_subparsers.add_parser(
        "create-with-content", help="Create a document with content in one step"
    )
    document_create_with_content.add_argument("--filestore-id", required=True, help="Filestore ID")
    document_create_with_content.add_argument("--name", required=True, help="Document name")
    document_create_with_content.add_argument("--doc-type", required=True, help="Document type")
    document_create_with_content.add_argument("--metadata", help="JSON string with document metadata")
    document_create_with_content.add_argument(
        "--content-file", required=True, help="Path to the content file"
    )
    document_create_with_content.add_argument(
        "--content-type", default=DEFAULT_CONTENT_TYPE, help=f"Content type (default: {DEFAULT_CONTENT_TYPE})"
    )
    document_create_with_content.add_argument(
        "--content-name", help="Name for the content object (defaults to document name)"
    )
    document_create_with_content.add_argument(
        "--comment", default="Initial version", help="Comment for the first version"
    )

    # Content commands
    content_parser = subparsers.add_parser("content", help="Operations on content objects")
    content_subparsers = content_parser.add_subparsers(dest="action", help="Action to perform on content objects")

    # Content - List
    content_list = content_subparsers.add_parser("list", help="List content objects")
    content_list.add_argument("--filestore-id", required=True, help="Filestore ID")
    content_list.add_argument("--name", help="Filter by content name")

    # Content - Create
    content_create = content_subparsers.add_parser("create", help="Create a new content object")
    content_create.add_argument("--filestore-id", required=True, help="Filestore ID")
    content_create.add_argument("--name", required=True, help="Content name")
    content_create.add_argument(
        "--content-type", default=DEFAULT_CONTENT_TYPE, help=f"Content type (default: {DEFAULT_CONTENT_TYPE})"
    )

    # Content - Upload
    content_upload = content_subparsers.add_parser("upload", help="Upload data to a content object")
    content_upload.add_argument("--id", required=True, help="Content ID")
    content_upload.add_argument("--file", required=True, help="Path to the file to upload")
    content_upload.add_argument(
        "--content-type", default=DEFAULT_CONTENT_TYPE, help=f"Content type (default: {DEFAULT_CONTENT_TYPE})"
    )

    # Content - Download
    content_download = content_subparsers.add_parser("download", help="Download content data")
    content_download.add_argument("--id", required=True, help="Content ID")
    content_download.add_argument("--output", required=True, help="Output file path")

    # Content - Delete
    content_delete = content_subparsers.add_parser("delete", help="Delete a content object")
    content_delete.add_argument("--id", required=True, help="Content ID")

    # Version commands
    version_parser = subparsers.add_parser("version", help="Operations on document versions")
    version_subparsers = version_parser.add_subparsers(dest="action", help="Action to perform on document versions")

    # Version - List
    version_list = version_subparsers.add_parser("list", help="List versions of a document")
    version_list.add_argument("--document-id", required=True, help="Document ID")

    # Version - Create
    version_create = version_subparsers.add_parser("create", help="Create a new document version")
    version_create.add_argument("--document-id", required=True, help="Document ID")
    version_create.add_argument("--content-id", required=True, help="Content ID")
    version_create.add_argument(
        "--version-number", type=int, required=True, help="Version number (should be incremental)"
    )
    version_create.add_argument("--metadata", help="JSON string with version metadata")

    # Version - Get
    version_get = version_subparsers.add_parser("get", help="Get version details")
    version_get.add_argument("--id", required=True, help="Version ID")

    # Global arguments
    parser.add_argument(
        "--base-url", 
        default=BASE_URL,
        help=f"Base URL for the Document Management API (default: {BASE_URL})"
    )
    parser.add_argument(
        "--format",
        choices=["json", "pretty"],
        default="pretty",
        help="Output format (default: pretty)"
    )

    return parser


def print_response(response_data: Union[Dict, List], args: argparse.Namespace) -> None:
    """Format and print the API response according to the specified format."""
    if args.format == "json":
        print(json.dumps(response_data))
    else:  # pretty
        print(json.dumps(response_data, indent=2))


def handle_api_error(response: requests.Response) -> None:
    """Handle API error responses."""
    try:
        error_data = response.json()
        print(f"Error: {response.status_code} - {response.reason}")
        print(json.dumps(error_data, indent=2))
    except ValueError:
        print(f"Error: {response.status_code} - {response.reason}")
        print(response.text)
    sys.exit(1)


def api_request(
    method: str,
    endpoint: str,
    base_url: str = BASE_URL,
    headers: Dict[str, str] = None,
    params: Dict[str, Any] = None,
    data: Dict[str, Any] = None,
    files: Dict[str, Any] = None,
) -> Dict[str, Any]:
    """Make an API request and handle common errors."""
    url = f"{base_url}/{endpoint}"
    
    if headers is None:
        headers = {}
    
    if data is not None and files is None:
        headers.update(JSON_HEADERS)
        data = json.dumps(data)
    
    try:
        response = requests.request(
            method=method,
            url=url,
            headers=headers,
            params=params,
            data=data,
            files=files,
        )
        
        if response.status_code >= 400:
            handle_api_error(response)
            
        if response.status_code == 204 or not response.text:
            return {"status": "success", "message": "Operation completed successfully"}
            
        return response.json()
    except requests.RequestException as e:
        print(f"Error connecting to API: {e}")
        sys.exit(1)
    except json.JSONDecodeError:
        if response.status_code < 400:
            return {"status": "success", "content": response.text}
        handle_api_error(response)
        return {}


def parse_metadata(metadata_str: Optional[str]) -> Dict[str, Any]:
    """Parse JSON metadata string into a dictionary."""
    if not metadata_str:
        return {}
    try:
        return json.loads(metadata_str)
    except json.JSONDecodeError:
        print("Error: Invalid JSON in metadata")
        sys.exit(1)


# Filestore commands
def list_filestores(args: argparse.Namespace) -> None:
    """List all filestores, optionally filtered by name."""
    params = {}
    if args.name:
        params["name"] = args.name
    
    response = api_request("GET", "filestores", args.base_url, params=params)
    print_response(response, args)


def create_filestore(args: argparse.Namespace) -> None:
    """Create a new filestore."""
    data = {
        "name": args.name,
    }
    if args.description:
        data["description"] = args.description
    
    response = api_request("POST", "filestores", args.base_url, data=data)
    print_response(response, args)


def get_filestore(args: argparse.Namespace) -> None:
    """Get details of a specific filestore."""
    response = api_request("GET", f"filestores/{args.id}", args.base_url)
    print_response(response, args)


def delete_filestore(args: argparse.Namespace) -> None:
    """Delete a filestore."""
    response = api_request("DELETE", f"filestores/{args.id}", args.base_url)
    print_response(response, args)


# Document commands
def list_documents(args: argparse.Namespace) -> None:
    """List documents in a filestore, optionally filtered by name."""
    params = {"filestoreId": args.filestore_id}
    if args.name:
        params["name"] = args.name
    
    response = api_request("GET", "documents", args.base_url, params=params)
    print_response(response, args)


def create_document(args: argparse.Namespace) -> None:
    """Create a new document."""
    data = {
        "filestoreId": args.filestore_id,
        "name": args.name,
        "documentType": args.doc_type,
    }
    if args.metadata:
        data["metadata"] = parse_metadata(args.metadata)
    
    response = api_request("POST", "documents", args.base_url, data=data)
    print_response(response, args)


def get_document(args: argparse.Namespace) -> None:
    """Get details of a specific document."""
    response = api_request("GET", f"documents/{args.id}", args.base_url)
    print_response(response, args)


def update_document(args: argparse.Namespace) -> None:
    """Update a document's properties."""
    data = {}
    if args.name:
        data["name"] = args.name
    if args.doc_type:
        data["documentType"] = args.doc_type
    if args.metadata:
        data["metadata"] = parse_metadata(args.metadata)
    
    if not data:
        print("Error: At least one property to update must be provided")
        sys.exit(1)
    
    response = api_request("PUT", f"documents/{args.id}", args.base_url, data=data)
    print_response(response, args)


def delete_document(args: argparse.Namespace) -> None:
    """Delete a document."""
    response = api_request("DELETE", f"documents/{args.id}", args.base_url)
    print_response(response, args)


def create_document_with_content(args: argparse.Namespace) -> None:
    """Create a document with content in one step."""
    # Check if content file exists
    if not os.path.isfile(args.content_file):
        print(f"Error: Content file '{args.content_file}' not found")
        sys.exit(1)
    
    # Step 1: Create document
    doc_data = {
        "filestoreId": args.filestore_id,
        "name": args.name,
        "documentType": args.doc_type,
    }
    if args.metadata:
        doc_data["metadata"] = parse_metadata(args.metadata)
    
    doc_response = api_request("POST", "documents", args.base_url, data=doc_data)
    document_id = doc_response.get("id")
    
    if not document_id:
        print("Error: Failed to create document")
        print_response(doc_response, args)
        sys.exit(1)
    
    print(f"Document created successfully with ID: {document_id}")
    
    # Step 2: Create content object
    content_name = args.content_name if args.content_name else args.name
    content_data = {
        "filestoreId": args.filestore_id,
        "name": content_name,
        "contentType": args.content_type,
    }
    
    content_response = api_request("POST", "contents", args.base_url, data=content_data)
    content_id = content_response.get("id")
    
    if not content_id:
        print("Error: Failed to create content object")
        print_response(content_response, args)
        sys.exit(1)
    
    print(f"Content object created successfully with ID: {content_id}")
    
    # Step 3: Upload content data
    with open(args.content_file, "rb") as file:
        files = {"file": (os.path.basename(args.content_file), file, args.content_type)}
        upload_response = api_request(
            "PUT", 
            f"contents/{content_id}/data", 
            args.base_url,
            files=files
        )
    
    print("Content data uploaded successfully")
    
    # Step 4: Create document version linking document and content
    version_data = {
        "documentId": document_id,
        "contentId": content_id,
        "versionNumber": 1,
        "metadata": {
            "comment": args.comment
        }
    }
    
    version_response = api_request("POST", "document-versions", args.base_url, data=version_data)
    version_id = version_response.get("id")
    
    if not version_id:
        print("Error: Failed to create document version")
        print_response(version_response, args)
        sys.exit(1)
    
    print(f"Document version created successfully with ID: {version_id}")
    
    # Output final result
    result = {
        "status": "success",
        "document": {
            "id": document_id,
            "name": args.name,
            "type": args.doc_type,
        },
        "content": {
            "id": content_id,
            "name": content_name,
            "type": args.content_type,
        },
        "version": {
            "id": version_id,
            "number": 1,
            "comment": args.comment,
        }
    }
    
    print_response(result, args)


# Content commands
def list_contents(args: argparse.Namespace) -> None:
    """List content objects in a filestore, optionally filtered by name."""
    params = {"filestoreId": args.filestore_id}
    if args.name:
        params["name"] = args.name
    
    response = api_request("GET", "contents", args.base_url, params=params)
    print_response(response, args)


def create_content(args: argparse.Namespace) -> None:
    """Create a new content object."""
    data = {
        "filestoreId": args.filestore_id,
        "name": args.name,
        "contentType": args.content_type,
    }
    
    response = api_request("POST", "contents", args.base_url, data=data)
    print_response(response, args)


def upload_content(args: argparse.Namespace) -> None:
    """Upload data to a content object."""
    if not os.path.isfile(args.file):
        print(f"Error: File '{args.file}' not found")
        sys.exit(1)
    
    with open(args.file, "rb") as file:
        files = {"file": (os.path.basename(args.file), file, args.content_type)}
        response = api_request(
            "PUT", 
            f"contents/{args.id}/data", 
            args.base_url,
            files=files
        )
    
    print_response(response, args)


def download_content(args: argparse.Namespace) -> None:
    """Download content data to a file."""
    headers = {}
    url = f"{args.base_url}/contents/{args.id}/data"
    
    try:
        response = requests.get(url, headers=headers, stream=True)
        
        if response.status_code >= 400:
            handle_api_error(response)
        
        with open(args.output, "wb") as file:
            for chunk in response.iter_content(chunk_size=8192):
                file.write(chunk)
        
        print(f"Content downloaded successfully to {args.output}")
    except requests.RequestException as e:
        print(f"Error downloading content: {e}")
        sys.exit(1)


def delete_content(args: argparse.Namespace) -> None:
    """Delete a content object."""
    response = api_request("DELETE", f"contents/{args.id}", args.base_url)
    print_response(response, args)


# Version commands
def list_versions(args: argparse.Namespace) -> None:
    """List versions of a document."""
    params = {"documentId": args.document_id}
    
    response = api_request("GET", "document-versions", args.base_url, params=params)
    print_response(response, args)


def create_version(args: argparse.Namespace) -> None:
    """Create a new document version."""
    data = {
        "documentId": args.document_id,
        "contentId": args.content_id,
        "versionNumber": args.version_number,
    }
    
    if args.metadata:
        data["metadata"] = parse_metadata(args.metadata)
    
    response = api_request("POST", "document-versions", args.base_url, data=data)
    print_response(response, args)


def get_version(args: argparse.Namespace) -> None:
    """Get details of a specific document version."""
    response = api_request("GET", f"document-versions/{args.id}", args.base_url)
    print_response(response, args)


def main():
    """Main function to parse arguments and execute commands."""
    parser = configure_parser()
    args = parser.parse_args()
    
    # If no entity or action is specified, show help
    if not args.entity:
        parser.print_help()
        sys.exit(1)
    
    # File store operations
    if args.entity == "filestore":
        if not args.action:
            parser.parse_args(["filestore", "--help"])
            sys.exit(1)
            
        if args.action == "list":
            list_filestores(args)
        elif args.action == "create":
            create_filestore(args)
        elif args.action == "get":
            get_filestore(args)
        elif args.action == "delete":
            delete_filestore(args)
    
    # Document operations
    elif args.entity == "document":
        if not args.action:
            parser.parse_args(["document", "--help"])
            sys.exit(1)
            
        if args.action == "list":
            list_documents(args)
        elif args.action == "create":
            create_document(args)
        elif args.action == "get":
            get_document(args)
        elif args.action == "update":
            update_document(args)
        elif args.action == "delete":
            delete_document(args)
        elif args.action == "create-with-content":
            create_document_with_content(args)
    
    # Content operations
    elif args.entity == "content":
        if not args.action:
            parser.parse_args(["content", "--help"])
            sys.exit(1)
            
        if args.action == "list":
            list_contents(args)
        elif args.action == "create":
            create_content(args)
        elif args.action == "upload":
            upload_content(args)
        elif args.action == "download":
            download_content(args)
        elif args.action == "delete":
            delete_content(args)
    
    # Version operations
    elif args.entity == "version":
        if not args.action:
            parser.parse_args(["version", "--help"])
            sys.exit(1)
            
        if args.action == "list":
            list_versions(args)
        elif args.action == "create":
            create_version(args)
        elif args.action == "get":
            get_version(args)


if __name__ == "__main__":
    main()
