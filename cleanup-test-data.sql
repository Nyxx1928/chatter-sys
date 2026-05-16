-- Cleanup Script for Test Data
-- Run this to delete old test accounts and pending registrations

-- 1. Show what will be deleted (run this first to review)
SELECT 'Users to delete:' as info;
SELECT id, username, email, email_verified, created_at 
FROM users 
WHERE email_verified = false
ORDER BY created_at DESC;

SELECT 'Pending registrations to delete:' as info;
SELECT id, username, email, email_sent, created_at, expiry_date
FROM pending_registrations
ORDER BY created_at DESC;

-- 2. Delete old unverified users (created by old system)
-- Uncomment these lines after reviewing above
-- DELETE FROM users WHERE email_verified = false;

-- 3. Delete all pending registrations
-- Uncomment this line after reviewing above
-- DELETE FROM pending_registrations;

-- 4. Verify cleanup
-- SELECT COUNT(*) as unverified_users FROM users WHERE email_verified = false;
-- SELECT COUNT(*) as pending_registrations FROM pending_registrations;
