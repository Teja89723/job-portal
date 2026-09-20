-- Drop user first if they exist
DROP USER IF EXISTS 'jobportal'@'%';
DROP USER IF EXISTS 'jobportal'@'localhost';

-- Create the user for BOTH localhost (unix socket) and % (TCP/any host,
-- e.g. from a JDBC connection to 127.0.0.1) so login works no matter how
-- the app connects to MySQL.
CREATE USER 'jobportal'@'localhost' IDENTIFIED BY 'jobportal';
CREATE USER 'jobportal'@'%' IDENTIFIED BY 'jobportal';

GRANT ALL PRIVILEGES ON * . * TO 'jobportal'@'localhost';
GRANT ALL PRIVILEGES ON * . * TO 'jobportal'@'%';

FLUSH PRIVILEGES;