-- The H2 database is shared by every test in the run and these tests commit for
-- real, so each test starts by clearing what the previous one left behind.
-- Deleted in foreign key order.
delete from repayment;
delete from loan;
delete from loan_offer;
delete from loan_application;
delete from users;
delete from lender;

insert into lender (name, available_amount, min_income, base_interest_rate, max_tenure_months, active)
values ('HDFC Bank', 5000000.00, 40000.00, 12.00, 36, true),
       ('ABC NBFC', 2000000.00, 25000.00, 13.50, 24, true),
       ('XYZ NBFC', 1000000.00, 15000.00, 15.00, 12, true);
