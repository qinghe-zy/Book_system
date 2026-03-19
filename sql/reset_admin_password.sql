USE library_system;
UPDATE users
SET password='$2a$10$MesFtkvcUTdGYEn8SYmWmubvhUrXRKsV1Mz.ycfZRNjDSiAtO7gAC',
    email='admin@library.com',
    role='ADMIN'
WHERE username='admin';
SELECT username,email,role,password FROM users WHERE username='admin';
