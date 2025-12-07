-- Migration script to add rendition support to Content table
-- This adds the new columns for primary/secondary renditions and indexable content
-- Run this if you need to manually update an existing database

-- Add is_primary column (defaults to true for existing content)
ALTER TABLE content ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT TRUE;

-- Add parent_rendition_id column (foreign key to content.id)
ALTER TABLE content ADD COLUMN IF NOT EXISTS parent_rendition_id BIGINT;

-- Add is_indexable column (defaults to false for existing content)
ALTER TABLE content ADD COLUMN IF NOT EXISTS is_indexable BOOLEAN NOT NULL DEFAULT FALSE;

-- Add foreign key constraint for parent_rendition
ALTER TABLE content ADD CONSTRAINT IF NOT EXISTS fk_parent_rendition 
    FOREIGN KEY (parent_rendition_id) REFERENCES content(id) ON DELETE CASCADE;

-- Verify the columns were added
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'CONTENT' 
AND column_name IN ('IS_PRIMARY', 'PARENT_RENDITION_ID', 'IS_INDEXABLE')
ORDER BY column_name;
