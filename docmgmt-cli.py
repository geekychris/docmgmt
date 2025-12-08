#!/usr/bin/env python3
"""
Document Management System CLI

A modern command-line interface for the Document Management System.
Uses the Python client library for all operations.
"""

import sys
import os
import argparse
import json
from pathlib import Path

# Add clients/python to path
sys.path.insert(0, str(Path(__file__).parent / 'clients' / 'python'))

from docmgmt_client import DocumentManagementClient


def main():
    parser = argparse.ArgumentParser(
        description='Document Management System CLI',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
Examples:
  # List all documents
  %(prog)s documents list
  
  # Create a document
  %(prog)s documents create --name "My Document" --type ARTICLE --description "Test doc"
  
  # Get document details
  %(prog)s documents get 1
  
  # Upload content to a document
  %(prog)s content upload --document-id 1 --file document.pdf
  
  # Download content
  %(prog)s content download 1 --output downloaded.pdf
  
  # Create a major version
  %(prog)s documents version-major 1
  
  # Search documents
  %(prog)s search "spring framework"
  
  # Rebuild search index
  %(prog)s search rebuild-index
        '''
    )
    
    parser.add_argument('--base-url', default='http://localhost:8082/docmgmt/api',
                       help='API base URL (default: %(default)s)')
    
    subparsers = parser.add_subparsers(dest='command', help='Command to execute')
    subparsers.required = True
    
    # =========================================================================
    # Documents commands
    # =========================================================================
    docs = subparsers.add_parser('documents', help='Document operations')
    docs_sub = docs.add_subparsers(dest='action')
    docs_sub.required = True
    
    # List documents
    docs_list = docs_sub.add_parser('list', help='List all latest documents')
    
    # Get document
    docs_get = docs_sub.add_parser('get', help='Get document by ID')
    docs_get.add_argument('id', type=int, help='Document ID')
    
    # Create document
    docs_create = docs_sub.add_parser('create', help='Create a new document')
    docs_create.add_argument('--name', required=True, help='Document name')
    docs_create.add_argument('--type', required=True,
                            choices=['ARTICLE', 'MANUAL', 'REPORT', 'SPREADSHEET', 
                                   'PRESENTATION', 'IMAGE', 'VIDEO', 'AUDIO', 'OTHER'],
                            help='Document type')
    docs_create.add_argument('--description', help='Description')
    docs_create.add_argument('--keywords', help='Keywords (space-separated)')
    docs_create.add_argument('--tags', nargs='+', help='Tags (space-separated list)')
    
    # Update document
    docs_update = docs_sub.add_parser('update', help='Update document')
    docs_update.add_argument('id', type=int, help='Document ID')
    docs_update.add_argument('--name', help='New name')
    docs_update.add_argument('--description', help='New description')
    docs_update.add_argument('--keywords', help='New keywords')
    docs_update.add_argument('--tags', nargs='+', help='New tags')
    
    # Delete document
    docs_delete = docs_sub.add_parser('delete', help='Delete document')
    docs_delete.add_argument('id', type=int, help='Document ID')
    
    # Create major version
    docs_major = docs_sub.add_parser('version-major', help='Create major version')
    docs_major.add_argument('id', type=int, help='Document ID')
    
    # Create minor version
    docs_minor = docs_sub.add_parser('version-minor', help='Create minor version')
    docs_minor.add_argument('id', type=int, help='Document ID')
    
    # Version history
    docs_history = docs_sub.add_parser('version-history', help='Get version history')
    docs_history.add_argument('id', type=int, help='Document ID')
    
    # =========================================================================
    # Content commands
    # =========================================================================
    content = subparsers.add_parser('content', help='Content operations')
    content_sub = content.add_subparsers(dest='action')
    content_sub.required = True
    
    # List content for document
    content_list = content_sub.add_parser('list', help='List content for document')
    content_list.add_argument('document_id', type=int, help='Document ID')
    
    # Get content metadata
    content_get = content_sub.add_parser('get', help='Get content metadata')
    content_get.add_argument('id', type=int, help='Content ID')
    
    # Upload content
    content_upload = content_sub.add_parser('upload', help='Upload content file')
    content_upload.add_argument('--document-id', type=int, required=True, help='Document ID')
    content_upload.add_argument('--file', required=True, help='File to upload')
    content_upload.add_argument('--store-in-db', action='store_true', 
                               help='Store in database (default: file system)')
    content_upload.add_argument('--filestore-id', type=int, help='File store ID (if not in DB)')
    
    # Download content
    content_download = content_sub.add_parser('download', help='Download content')
    content_download.add_argument('id', type=int, help='Content ID')
    content_download.add_argument('--output', required=True, help='Output file path')
    
    # Delete content
    content_delete = content_sub.add_parser('delete', help='Delete content')
    content_delete.add_argument('id', type=int, help='Content ID')
    
    # =========================================================================
    # Search commands
    # =========================================================================
    search = subparsers.add_parser('search', help='Search operations')
    search_sub = search.add_subparsers(dest='action')
    search_sub.required = True
    
    # Simple search
    search_query = search_sub.add_parser('query', help='Search documents')
    search_query.add_argument('query', help='Search query')
    search_query.add_argument('--limit', type=int, default=50, help='Max results')
    
    # Field search
    search_fields = search_sub.add_parser('fields', help='Field-specific search')
    search_fields.add_argument('--name', help='Search in name field')
    search_fields.add_argument('--description', help='Search in description')
    search_fields.add_argument('--keywords', help='Search in keywords')
    search_fields.add_argument('--tags', help='Search in tags')
    search_fields.add_argument('--content', help='Search in content')
    search_fields.add_argument('--operator', choices=['AND', 'OR'], default='AND',
                              help='Boolean operator (default: AND)')
    search_fields.add_argument('--limit', type=int, default=50, help='Max results')
    
    # Rebuild index
    search_rebuild = search_sub.add_parser('rebuild-index', help='Rebuild search index')
    
    # Index stats
    search_stats = search_sub.add_parser('stats', help='Get index statistics')
    
    # =========================================================================
    # File Store commands
    # =========================================================================
    filestore = subparsers.add_parser('filestores', help='File store operations')
    filestore_sub = filestore.add_subparsers(dest='action')
    filestore_sub.required = True
    
    # List file stores
    filestore_list = filestore_sub.add_parser('list', help='List file stores')
    filestore_list.add_argument('--active-only', action='store_true', 
                                help='Show only active file stores')
    
    # Get file store
    filestore_get = filestore_sub.add_parser('get', help='Get file store details')
    filestore_get.add_argument('id', type=int, help='File store ID')
    
    # Create file store
    filestore_create = filestore_sub.add_parser('create', help='Create file store')
    filestore_create.add_argument('--name', required=True, help='File store name')
    filestore_create.add_argument('--root-path', required=True, help='Root path')
    filestore_create.add_argument('--status', choices=['ACTIVE', 'INACTIVE'], 
                                  default='ACTIVE', help='Status')
    
    # Parse arguments
    args = parser.parse_args()
    
    # Initialize client
    client = DocumentManagementClient(base_url=args.base_url)
    
    try:
        # Execute command
        if args.command == 'documents':
            handle_documents(client, args)
        elif args.command == 'content':
            handle_content(client, args)
        elif args.command == 'search':
            handle_search(client, args)
        elif args.command == 'filestores':
            handle_filestores(client, args)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


def handle_documents(client, args):
    """Handle document commands."""
    if args.action == 'list':
        docs = client.list_latest_documents()
        print_json(docs)
    
    elif args.action == 'get':
        doc = client.get_document(args.id)
        print_json(doc)
    
    elif args.action == 'create':
        doc = client.create_document(
            name=args.name,
            document_type=args.type,
            description=args.description,
            keywords=args.keywords,
            tags=args.tags
        )
        print_json(doc)
        print(f"\n✓ Created document ID: {doc['id']}", file=sys.stderr)
    
    elif args.action == 'update':
        # Get current document
        current = client.get_document(args.id)
        
        # Update fields
        if args.name:
            current['name'] = args.name
        if args.description:
            current['description'] = args.description
        if args.keywords:
            current['keywords'] = args.keywords
        if args.tags:
            current['tags'] = args.tags
        
        doc = client.update_document(args.id, current)
        print_json(doc)
        print(f"\n✓ Updated document ID: {args.id}", file=sys.stderr)
    
    elif args.action == 'delete':
        client.delete_document(args.id)
        print(f"✓ Deleted document ID: {args.id}", file=sys.stderr)
    
    elif args.action == 'version-major':
        doc = client.create_major_version(args.id)
        print_json(doc)
        print(f"\n✓ Created major version: {doc['versionLabel']}", file=sys.stderr)
    
    elif args.action == 'version-minor':
        doc = client.create_minor_version(args.id)
        print_json(doc)
        print(f"\n✓ Created minor version: {doc['versionLabel']}", file=sys.stderr)
    
    elif args.action == 'version-history':
        history = client.get_version_history(args.id)
        print_json(history)


def handle_content(client, args):
    """Handle content commands."""
    if args.action == 'list':
        contents = client.get_content_by_sysobject(args.document_id)
        print_json(contents)
    
    elif args.action == 'get':
        content = client.get_content_metadata(args.id)
        print_json(content)
    
    elif args.action == 'upload':
        content = client.upload_content_file(
            file_path=args.file,
            sysobject_id=args.document_id,
            store_in_database=args.store_in_db,
            file_store_id=args.filestore_id
        )
        print_json(content)
        print(f"\n✓ Uploaded content ID: {content['id']}", file=sys.stderr)
    
    elif args.action == 'download':
        client.download_content(args.id, args.output)
        print(f"✓ Downloaded to: {args.output}", file=sys.stderr)
    
    elif args.action == 'delete':
        client.delete_content(args.id)
        print(f"✓ Deleted content ID: {args.id}", file=sys.stderr)


def handle_search(client, args):
    """Handle search commands."""
    if args.action == 'query':
        results = client.search(args.query, args.limit)
        print_json(results)
        print(f"\n✓ Found {len(results)} results", file=sys.stderr)
    
    elif args.action == 'fields':
        field_queries = {}
        if args.name:
            field_queries['name'] = args.name
        if args.description:
            field_queries['description'] = args.description
        if args.keywords:
            field_queries['keywords'] = args.keywords
        if args.tags:
            field_queries['tags'] = args.tags
        if args.content:
            field_queries['content'] = args.content
        
        if not field_queries:
            print("Error: At least one field query is required", file=sys.stderr)
            sys.exit(1)
        
        results = client.search_fields(field_queries, args.operator, args.limit)
        print_json(results)
        print(f"\n✓ Found {len(results)} results", file=sys.stderr)
    
    elif args.action == 'rebuild-index':
        result = client.rebuild_search_index()
        print(f"✓ {result}", file=sys.stderr)
    
    elif args.action == 'stats':
        stats = client.get_search_stats()
        print_json(stats)


def handle_filestores(client, args):
    """Handle file store commands."""
    if args.action == 'list':
        if args.active_only:
            stores = client.list_active_filestores()
        else:
            stores = client.list_filestores()
        print_json(stores)
    
    elif args.action == 'get':
        store = client.get_filestore(args.id)
        print_json(store)
    
    elif args.action == 'create':
        store = client.create_filestore(
            name=args.name,
            root_path=args.root_path,
            status=args.status
        )
        print_json(store)
        print(f"\n✓ Created file store ID: {store['id']}", file=sys.stderr)


def print_json(data):
    """Print data as formatted JSON."""
    print(json.dumps(data, indent=2))


if __name__ == '__main__':
    main()
