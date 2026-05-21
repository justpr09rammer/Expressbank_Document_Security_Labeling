# Expressbank Document Security Labeling

A simple Java desktop application that automatically detects sensitive information in documents and assigns security labels.

## Features

- Automatic document classification
- Detects:
  - Credit card numbers
  - Passwords
  - API keys
  - Email addresses
  - Phone numbers
  - Bank account information
- Manual security labeling
- Audit logging
- Supports:
  - .Docx
  - Word documents
  - Excel files
  - Text files

## Security Labels

- PUBLIC
- INTERNAL_ONLY
- CONFIDENTIAL
- RESTRICTED

## Requirements

- Java 11+
- Maven
- JavaFX

## Installation

Clone the repository:

```bash
git clone https://github.com/justpr09rammer/Expressbank_Document_Security_Labeling.git
cd Expressbank_Document_Security_Labeling
```

Run with Maven:

```bash
mvn clean install
mvn javafx:run
```

## Usage

1. Launch the application
2. Select a document
3. Choose:
   - Auto Label
   - Manual Label
4. View detected matches and assigned security level

## Example Detected Data

```text
4532123412345678
password=admin123
api_key=sk_test_123456
admin@gmail.com
```

## Project Structure

```text
src/main/java/org/example/
├── model/
├── service/
└── utils/
```

## Technologies Used

- Java
- JavaFX
- Maven
- Apache POI
- PDFBox
- Regex pattern matching

## Future Improvements

- OCR support
- Machine learning classification
- Deep learning models
- Export audit reports
- Custom rule editor
