CREATE DATABASE IF NOT EXISTS elms_db;

USE elms_db;

CREATE TABLE IF NOT EXISTS employees (
    emp_id INT PRIMARY KEY AUTO_INCREMENT,
    emp_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    department VARCHAR(50),
    leave_balance INT DEFAULT 20
);

CREATE TABLE IF NOT EXISTS leave_requests (
    leave_id INT PRIMARY KEY AUTO_INCREMENT,
    emp_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    days INT NOT NULL,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
);

SHOW TABLES;
INSERT INTO employees
(emp_name, email, department, leave_balance)
VALUES
('Veeraj', 'veeraj@gmail.com', 'IT', 20),
('Arun', 'arun@gmail.com', 'HR', 20),
('Priya', 'priya@gmail.com', 'Finance', 20);
SELECT * FROM employees;