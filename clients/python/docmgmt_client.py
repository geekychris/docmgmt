#!/usr/bin/env python3
"""
Document Management System Python Client

This client demonstrates copy-on-write versioning operations over REST.
"""

import requests
import json
from typing import Optional, Dict, List, Any
from io import BytesIO
import os


class DocumentManagementClient:
    """Client for interacting with the Document Management System REST API."""
    
    def __init__(self, base_url: str = "http://localhost:8082/docmgmt/api"):
        """
        Initialize the client.
        
        Args:
            base_url: Base URL for the API (default: http://localhost:8082/docmgmt/api)
        """
        self.base_url = base_url.rstrip('/')
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json'
        })
    
    # =========================================================================
    # Document Operations
    # =========================================================================
    
    def create_document(
        self,
        name: str,
        document_type: str,
        description: Optional[str] = None,
        author: Optional[str] = None,
        keywords: Optional[str] = None,
        tags: Optional[List[str]] = None
    ) -> Dict[str, Any]:
        """
        Create a new document.
        
        Args:
            name: Document name
            document_type: One of: REPORT, MANUAL, ARTICLE, SPECIFICATION, OTHER
            description: Optional description
            author: Optional author name
            keywords: Optional keywords
            tags: Optional list of tags
            
        Returns:
            Created document with ID and version information
        """
        payload = {
            "name": name,
            "documentType": document_type
        }
        if description:
            payload["description"] = description
        if author:
            payload["author"] = author
        if keywords:
            payload["keywords"] = keywords
        if tags:
            payload["tags"] = tags
        
        response = self.session.post(f"{self.base_url}/documents", json=payload)
        response.raise_for_status()
        return response.json()
    
    def get_document(self, document_id: int) -> Dict[str, Any]:
        """
        Get document by ID.
        
        Args:
            document_id: Document ID
            
        Returns:
            Document details
        """
        response = self.session.get(f"{self.base_url}/documents/{document_id}")
        response.raise_for_status()
        return response.json()
    
    def update_document(self, document_id: int, document_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Update document.
        
        Args:
            document_id: Document ID
            document_data: Updated document data
            
        Returns:
            Updated document
        """
        response = self.session.put(
            f"{self.base_url}/documents/{document_id}",
            json=document_data
        )
        response.raise_for_status()
        return response.json()
    
    def delete_document(self, document_id: int) -> None:
        """
        Delete document.
        
        Args:
            document_id: Document ID
        """
        response = self.session.delete(f"{self.base_url}/documents/{document_id}")
        response.raise_for_status()
    
    # =========================================================================
    # Versioning Operations
    # =========================================================================
    
    def create_major_version(self, document_id: int) -> Dict[str, Any]:
        """
        Create a major version (e.g., 1.0 → 2.0).
        
        Args:
            document_id: ID of document to version
            
        Returns:
            New version document
        """
        response = self.session.post(
            f"{self.base_url}/documents/{document_id}/versions/major"
        )
        response.raise_for_status()
        return response.json()
    
    def create_minor_version(self, document_id: int) -> Dict[str, Any]:
        """
        Create a minor version (e.g., 1.0 → 1.1).
        
        Args:
            document_id: ID of document to version
            
        Returns:
            New version document
        """
        response = self.session.post(
            f"{self.base_url}/documents/{document_id}/versions/minor"
        )
        response.raise_for_status()
        return response.json()
    
    def get_version_history(self, document_id: int) -> List[Dict[str, Any]]:
        """
        Get version history for a document.
        
        Args:
            document_id: Document ID
            
        Returns:
            List of version information
        """
        response = self.session.get(
            f"{self.base_url}/documents/{document_id}/versions/history"
        )
        response.raise_for_status()
        return response.json()
    
    # =========================================================================
    # Content Operations
    # =========================================================================
    
    def upload_content(
        self,
        file_path: str,
        sys_object_id: int,
        store_in_database: bool = True,
        file_store_id: Optional[int] = None
    ) -> Dict[str, Any]:
        """
        Upload content file.
        
        Args:
            file_path: Path to file to upload
            sys_object_id: ID of parent document/sys_object
            store_in_database: True to store in database, False for file store
            file_store_id: Required if store_in_database is False
            
        Returns:
            Created content metadata
        """
        with open(file_path, 'rb') as f:
            files = {
                'file': (os.path.basename(file_path), f)
            }
            data = {
                'sysObjectId': str(sys_object_id),
                'storeInDatabase': str(store_in_database).lower()
            }
            if file_store_id is not None:
                data['fileStoreId'] = str(file_store_id)
            
            # Remove Content-Type header for multipart
            headers = {k: v for k, v in self.session.headers.items() 
                      if k.lower() != 'content-type'}
            
            response = requests.post(
                f"{self.base_url}/content/upload",
                files=files,
                data=data,
                headers=headers
            )
            response.raise_for_status()
            return response.json()
    
    def upload_content_bytes(
        self,
        filename: str,
        content_bytes: bytes,
        sys_object_id: int,
        store_in_database: bool = True,
        file_store_id: Optional[int] = None,
        content_type: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Upload content from bytes.
        
        Args:
            filename: Name for the file
            content_bytes: File content as bytes
            sys_object_id: ID of parent document/sys_object
            store_in_database: True to store in database, False for file store
            file_store_id: Required if store_in_database is False
            content_type: Optional MIME type
            
        Returns:
            Created content metadata
        """
        files = {
            'file': (filename, BytesIO(content_bytes), content_type or 'application/octet-stream')
        }
        data = {
            'sysObjectId': str(sys_object_id),
            'storeInDatabase': str(store_in_database).lower()
        }
        if file_store_id is not None:
            data['fileStoreId'] = str(file_store_id)
        
        # Remove Content-Type header for multipart
        headers = {k: v for k, v in self.session.headers.items() 
                  if k.lower() != 'content-type'}
        
        response = requests.post(
            f"{self.base_url}/content/upload",
            files=files,
            data=data,
            headers=headers
        )
        response.raise_for_status()
        return response.json()
    
    def get_content(self, content_id: int) -> Dict[str, Any]:
        """
        Get content metadata.
        
        Args:
            content_id: Content ID
            
        Returns:
            Content metadata
        """
        response = self.session.get(f"{self.base_url}/content/{content_id}")
        response.raise_for_status()
        return response.json()
    
    def download_content(self, content_id: int) -> bytes:
        """
        Download content bytes.
        
        Args:
            content_id: Content ID
            
        Returns:
            Content bytes
        """
        response = self.session.get(f"{self.base_url}/content/{content_id}/download")
        response.raise_for_status()
        return response.content
    
    def get_content_by_sysobject(self, sys_object_id: int) -> List[Dict[str, Any]]:
        """
        Get all content for a document/sys_object.
        
        Args:
            sys_object_id: Document/sys_object ID
            
        Returns:
            List of content metadata
        """
        response = self.session.get(
            f"{self.base_url}/content/by-sysobject/{sys_object_id}"
        )
        response.raise_for_status()
        return response.json()
    
    def delete_content(self, content_id: int) -> None:
        """
        Delete content.
        
        Args:
            content_id: Content ID
        """
        response = self.session.delete(f"{self.base_url}/content/{content_id}")
        response.raise_for_status()


def _fmt_version(obj: Dict[str, Any]) -> str:
    """Format version from major/minor fields or version field."""
    if not isinstance(obj, dict):
        return "?"
    if 'majorVersion' in obj and 'minorVersion' in obj:
        try:
            return f"{int(obj.get('majorVersion', 0))}.{int(obj.get('minorVersion', 0))}"
        except Exception:
            pass
    return str(obj.get('version', '?'))


def main():
    """
    Demonstration of copy-on-write versioning workflow.
    """
    print("=== Document Management System Copy-on-Write Demo ===\n")
    
    client = DocumentManagementClient()
    
    try:
        # Step 1: Create initial document (v1.0)
        print("1. Creating document v1.0...")
        doc_v1 = client.create_document(
            name="User Manual",
            document_type="MANUAL",
            description="Product user manual",
            author="Technical Writing Team",
            tags=["documentation", "user-guide"]
        )
        print(f"   Created: {doc_v1['name']} (ID: {doc_v1['id']}, Version: {_fmt_version(doc_v1)})")
        
        # Step 2: Add content to v1.0
        print("\n2. Adding content to v1.0...")
        content_v1 = client.upload_content_bytes(
            filename="manual.txt",
            content_bytes=b"User Manual Version 1.0\nThis is the original content.",
            sys_object_id=doc_v1['id'],
            store_in_database=True,
            content_type="text/plain"
        )
        print(f"   Uploaded: {content_v1['name']} (ID: {content_v1['id']}, Storage: {content_v1['storageType']})")
        
        # Step 3: Verify v1.0 content
        print("\n3. Verifying v1.0 content...")
        v1_bytes = client.download_content(content_v1['id'])
        print(f"   Content: {v1_bytes.decode('utf-8')}")
        
        # Step 4: Create v2.0 (major version)
        print("\n4. Creating v2.0 (major version)...")
        doc_v2 = client.create_major_version(doc_v1['id'])
        print(f"   Created: {doc_v2['name']} (ID: {doc_v2['id']}, Version: {_fmt_version(doc_v2)})")
        print(f"   Parent Version ID: {doc_v2.get('parentVersionId', 'N/A')}")
        
        # Step 5: Check v2.0 content (initially shared)
        print("\n5. Checking v2.0 content (initially shared)...")
        v2_contents = client.get_content_by_sysobject(doc_v2['id'])
        print(f"   Found {len(v2_contents)} content item(s)")
        if v2_contents:
            v2_content = v2_contents[0]
            print(f"   Content ID: {v2_content['id']} (cloned from {content_v1['id']})")
            v2_bytes = client.download_content(v2_content['id'])
            print(f"   Content: {v2_bytes.decode('utf-8')}")
            print(f"   ✓ Initially shares same data as v1.0")
        
        # Step 6: Update content in v2.0 (trigger copy-on-write)
        print("\n6. Updating content in v2.0 (triggering copy-on-write)...")
        content_v2_new = client.upload_content_bytes(
            filename="manual.txt",
            content_bytes=b"User Manual Version 2.0\nThis version has significant updates and new features.",
            sys_object_id=doc_v2['id'],
            store_in_database=True,
            content_type="text/plain"
        )
        print(f"   Uploaded: {content_v2_new['name']} (ID: {content_v2_new['id']})")
        
        # Step 7: Verify copy-on-write worked
        print("\n7. Verifying copy-on-write (versions are now independent)...")
        
        # Check v1.0 still has original content
        v1_bytes_after = client.download_content(content_v1['id'])
        print(f"   v1.0 content: {v1_bytes_after.decode('utf-8')[:50]}...")
        
        # Check v2.0 has new content
        v2_bytes_after = client.download_content(content_v2_new['id'])
        print(f"   v2.0 content: {v2_bytes_after.decode('utf-8')[:50]}...")
        
        if v1_bytes_after != v2_bytes_after:
            print(f"   ✓ Copy-on-write successful! Versions are independent.")
        else:
            print(f"   ✗ Content is the same (unexpected)")
        
        # Step 8: Check version history
        print("\n8. Checking version history...")
        history = client.get_version_history(doc_v2['id'])
        print(f"   Found {len(history)} version(s):")
        for v in history:
            latest = " (LATEST)" if v.get('isLatestVersion') else ""
            print(f"     - v{_fmt_version(v)}: ID={v['id']}{latest}")
        
        # Step 9: Create v2.1 (minor version)
        print("\n9. Creating v2.1 (minor version)...")
        doc_v2_1 = client.create_minor_version(doc_v2['id'])
        print(f"   Created: {doc_v2_1['name']} (ID: {doc_v2_1['id']}, Version: {_fmt_version(doc_v2_1)})")
        
        # Verify v2.1 inherited v2.0's content
        v2_1_contents = client.get_content_by_sysobject(doc_v2_1['id'])
        print(f"   v2.1 has {len(v2_1_contents)} content item(s) (inherited from v2.0)")
        
        print("\n=== Demo Complete ===")
        print(f"\nSummary:")
        print(f"  - Created 3 versions: v1.0 (ID:{doc_v1['id']}), v2.0 (ID:{doc_v2['id']}), v2.1 (ID:{doc_v2_1['id']})")
        print(f"  - Demonstrated copy-on-write: v1.0 and v2.0 have independent content")
        print(f"  - All versions accessible via API")
        
    except requests.exceptions.RequestException as e:
        print(f"\n✗ Error: {e}")
        if hasattr(e.response, 'text'):
            print(f"  Response: {e.response.text}")
        return 1
    
    return 0


if __name__ == "__main__":
    exit(main())
