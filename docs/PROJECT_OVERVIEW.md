# Payment Service — Project Overview (plain English)

## What is it, in one sentence?

It's the software behind a "Pay now" button — a system that **takes payments, keeps track of
them, and gives money back (refunds) when needed** — plus an admin website where staff can see
and manage all those payments.

## A real-world analogy

Think of a shop's till (cash register) connected to a back office:

- The **till** is the API — it records every sale, gives each one a receipt number, and never
  charges you twice for the same tap of your card.
- The **back office** is the admin website — staff can search payments, check their status,
  and issue refunds.
- The **ledger** is the database — a permanent, tamper-evident record of every transaction.

## What can it actually do today?

1. **Take a payment** — record an amount, currency, and payment method. Each payment gets a
   unique id and starts as *Pending*, then becomes *Completed* or *Failed*.
2. **Never double-charge** — if the same request is sent twice (e.g. the user double-clicks or
   the network hiccups), it recognises the duplicate and returns the original payment instead
   of creating a second one. (This is called *idempotency*.)
3. **Issue refunds** — give money back against a payment, with the reason recorded.
4. **Search & filter** — admins can list payments by status, currency, or method.
5. **Stay safe under pressure** — if too many requests arrive too fast, it politely asks the
   caller to slow down instead of falling over.
6. **Explain itself** — every request gets a tracking number so any problem can be traced
   through the logs, and the whole API is documented in an interactive web page (Swagger).
7. **Report its health** — it continuously publishes metrics (how fast, how many errors) that
   monitoring tools can read.

## How is it built? (no jargon)

| Part | What it is | Everyday equivalent |
|---|---|---|
| **Backend (Java / Spring Boot)** | The brain — all the rules and the till logic | The cashier who follows shop policy |
| **Frontend (React)** | The admin website | The back-office screen staff look at |
| **Database (PostgreSQL)** | Where every payment is stored | The permanent ledger book |
| **Security** | Checks who you are and what you're allowed to do | The lock on the back-office door |

## Where is it heading? (the upgrade plan)

The goal is to make it work like a system a real company would run:

- **Real card processing** — plug into Stripe so payments are actually charged, not just
  recorded.
- **Background workers** — when a refund is requested, hand it to a queue so the website stays
  fast and the refund is processed reliably in the background (with automatic retries).
- **Email confirmations** — notify customers when something happens.
- **PDF receipts** — generate and store a proper receipt for each payment.
- **Run on the cloud (AWS)** — host it on professional infrastructure with proper login
  (Cognito), managed database (RDS), secure secret storage, and full monitoring.
- **Automatic deployment** — every code change is automatically tested and shipped (CI/CD),
  with the whole cloud setup defined as code (Terraform).

The detailed plan lives in [ROADMAP.md](ROADMAP.md), the progress checklist in
[PROGRESS.md](PROGRESS.md), and the cloud blueprint in [aws-architecture.md](aws-architecture.md).

## Why does this project matter (for a resume)?

It shows the full picture of building real software, not just code: **clean design, safety
under load, third-party integrations, cloud architecture, and automated delivery** — the same
concerns a real payments team deals with every day.
