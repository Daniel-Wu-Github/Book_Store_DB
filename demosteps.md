Part 1: Customer Workflow
Registration & Login:

Start at Login screen. Click "Register".

Create a new user (e.g., demo_user).

Login with demo_user. Show that the login works.

Catalog & Search:

Type a keyword (e.g., "Harry") in the search bar.

Highlight: Point out the table columns, specifically mentioning "We can see the Title, Author, Purchase Price, and Rental Price."

Cart & Ordering:

Select a book. Choose "BUY" in the dropdown. Click "Add".

Select a different book. Choose "RENT". Set days to "5". Click "Add".

Show the items in the cart table.

Click "Place Order".

Show the success message.

Part 2: Email System (Simulated)
Console Verification:

Alt-Tab to your IDE console/terminal.

Show the log output from DevEmailService or SmtpEmailService that says something like: "Sending email to demo_user@test.com... Bill details...".

This proves the Immediate Notification requirement.

Part 3: Manager Workflow
Manager Access:

Logout.

Login as a manager (e.g., admin_user).

Book Management:

Go to the "Books" tab.

Create: Click "New Book". Add "The Great Demo".

Edit: Select "The Great Demo". Click "Edit". Change price or stock. Save. Show the table updated.

Delete: Select the book and Click "Delete".

Order Management:

Go to the "Orders" tab.

Locate the order placed in Part 1.

Explain the columns (User, Status, Payment).

Click "Mark Paid". Show the status change to "PAID".
