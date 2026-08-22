-- V4__add_task_folder_path.sql
-- Add editable folder path field for task progress tracking.

ALTER TABLE tasks
ADD COLUMN folder_path TEXT;
