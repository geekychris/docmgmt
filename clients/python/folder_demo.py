#!/usr/bin/env python3
"""
Folder Hierarchy Demo

Demonstrates hierarchical folder organization and document management.
"""

import sys
from docmgmt_client import DocumentManagementClient


def main():
    print("=== Document Management System - Folder Hierarchy Demo ===\n")
    
    client = DocumentManagementClient()
    
    try:
        # Step 1: Create root folders
        print("1. Creating root folder structure...")
        projects = client.create_folder(
            name="Projects",
            path="/projects",
            description="All project folders",
            is_public=False,
            permissions=["READ", "WRITE"]
        )
        print(f"   Created root folder: {projects['name']} (ID: {projects['id']})")
        
        archive = client.create_folder(
            name="Archive",
            path="/archive",
            description="Archived documents",
            is_public=True
        )
        print(f"   Created root folder: {archive['name']} (ID: {archive['id']})")
        
        # Step 2: Create subfolder hierarchy
        print("\n2. Creating project subfolder hierarchy...")
        q4_folder = client.create_folder(
            name="Q4-2024",
            path="/projects/q4-2024",
            description="Q4 2024 projects"
        )
        print(f"   Created: {q4_folder['name']} (ID: {q4_folder['id']})")
        
        # Link Q4 folder under Projects
        client.add_child_folder(projects['id'], q4_folder['id'])
        print(f"   Linked {q4_folder['name']} as child of {projects['name']}")
        
        reports_folder = client.create_folder(
            name="Reports",
            path="/projects/q4-2024/reports",
            description="Q4 Reports"
        )
        print(f"   Created: {reports_folder['name']} (ID: {reports_folder['id']})")
        
        # Link Reports folder under Q4
        client.add_child_folder(q4_folder['id'], reports_folder['id'])
        print(f"   Linked {reports_folder['name']} as child of {q4_folder['name']}")
        
        # Step 3: Create documents
        print("\n3. Creating documents...")
        doc1 = client.create_document(
            name="Q4 Financial Report",
            document_type="REPORT",
            author="Finance Team",
            description="Quarterly financial analysis",
            tags=["finance", "q4", "2024"]
        )
        print(f"   Created: {doc1['name']} (ID: {doc1['id']})")
        
        doc2 = client.create_document(
            name="Project Status Summary",
            document_type="REPORT",
            author="Project Manager",
            description="Overall project status",
            tags=["status", "q4", "2024"]
        )
        print(f"   Created: {doc2['name']} (ID: {doc2['id']})")
        
        doc3 = client.create_document(
            name="Archived Spec",
            document_type="MANUAL",
            author="Tech Writer",
            description="Old specification document"
        )
        print(f"   Created: {doc3['name']} (ID: {doc3['id']})")
        
        # Step 4: Organize documents in folders
        print("\n4. Adding documents to folders...")
        client.add_item_to_folder(reports_folder['id'], doc1['id'])
        print(f"   Added '{doc1['name']}' to {reports_folder['name']}")
        
        client.add_item_to_folder(reports_folder['id'], doc2['id'])
        print(f"   Added '{doc2['name']}' to {reports_folder['name']}")
        
        client.add_item_to_folder(archive['id'], doc3['id'])
        print(f"   Added '{doc3['name']}' to {archive['name']}")
        
        # Step 5: Document in multiple folders
        print("\n5. Adding document to multiple folders...")
        client.add_item_to_folder(archive['id'], doc1['id'])
        print(f"   Added '{doc1['name']}' to {archive['name']} (also in {reports_folder['name']})")
        print(f"   Document now exists in 2 folders!")
        
        # Step 6: View folder contents
        print("\n6. Viewing folder contents...")
        reports_items = client.get_folder_items(reports_folder['id'])
        print(f"   {reports_folder['name']} contains {len(reports_items)} items:")
        for item in reports_items:
            print(f"     - {item['name']} (ID: {item['id']})")
        
        archive_items = client.get_folder_items(archive['id'])
        print(f"   {archive['name']} contains {len(archive_items)} items:")
        for item in archive_items:
            print(f"     - {item['name']} (ID: {item['id']})")
        
        # Step 7: Get root folders
        print("\n7. Listing root folders...")
        root_folders = client.get_root_folders()
        print(f"   Found {len(root_folders)} root folder(s):")
        for folder in root_folders:
            if folder['name'] in ['Projects', 'Archive']:  # Filter to our folders
                print(f"     - {folder['name']} (ID: {folder['id']})")
        
        # Step 8: Get folder hierarchy
        print("\n8. Getting full folder hierarchy...")
        hierarchy = client.get_folder_hierarchy(projects['id'])
        print(f"   Projects hierarchy contains {len(hierarchy)} folder(s):")
        for folder in hierarchy:
            # Estimate depth by counting path separators
            depth = folder.get('path', '').count('/') - 1 if folder.get('path') else 0
            indent = "  " * depth
            print(f"     {indent}- {folder['name']}")
        
        # Step 9: Version a document in a folder
        print("\n9. Creating document version (document stays in folders)...")
        doc1_v2 = client.create_major_version(doc1['id'])
        print(f"   Created v{doc1_v2['majorVersion']}.{doc1_v2['minorVersion']} of '{doc1['name']}'")
        print(f"   Original v{doc1['majorVersion']}.{doc1['minorVersion']} remains in folders")
        
        # Step 10: Remove document from a folder
        print("\n10. Removing document from one folder...")
        client.remove_item_from_folder(archive['id'], doc1['id'])
        print(f"   Removed '{doc1['name']}' from {archive['name']}")
        print(f"   Document still exists in {reports_folder['name']}")
        
        archive_items_after = client.get_folder_items(archive['id'])
        print(f"   {archive['name']} now contains {len(archive_items_after)} items")
        
        print("\n=== Demo Complete ===")
        print("\nFolder Organization Summary:")
        print("  ✓ Created hierarchical folder structure (Projects/Q4-2024/Reports)")
        print("  ✓ Organized documents in folders")
        print("  ✓ Demonstrated documents in multiple folders")
        print("  ✓ Queried folder contents and hierarchy")
        print("  ✓ Managed folder membership")
        
        return 0
        
    except Exception as e:
        print(f"\n✗ Error: {e}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
