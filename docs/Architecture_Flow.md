# BalanceX: Application Architecture Flow

This flowchart illustrates how data will move through the BalanceX app once all the new features—including the Local LLM, encrypted Room database, and UI updates—are implemented.

```mermaid
flowchart TD
    %% Styling
    classDef default fill:#f9f9f9,stroke:#333,stroke-width:2px,color:#000
    classDef ai fill:#e1f5fe,stroke:#0288d1,stroke-width:2px,color:#000
    classDef data fill:#e8f5e9,stroke:#388e3c,stroke-width:2px,color:#000
    classDef ui fill:#fff3e0,stroke:#f57c00,stroke-width:2px,color:#000
    classDef logic fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#000
    classDef user fill:#ffebee,stroke:#d32f2f,stroke-width:3px,color:#000

    %% User
    User((👤 User)):::user
    
    subgraph App [BalanceX Android Application]
        direction TB
        
        %% Authentication
        Auth{Biometric<br/>Auth}:::logic

        %% UI Layer
        subgraph UI_Layer [UI Layer (Material 3 XML & Jetpack Compose)]
            direction LR
            Dashboard[📊 Dashboard / Home]:::ui
            ManualEntry[✍️ Manual Entry Form]:::ui
            SmartEntry[🤖 Smart Add (Text/Voice)]:::ui
            ReceiptScan[📷 Scan Receipt]:::ui
            Goals[🏆 Goals & Gamification]:::ui
        end

        %% AI Intelligence Layer
        subgraph AI_Layer [On-Device Intelligence Layer]
            direction TB
            OCR[ML Kit Text Recognition]:::ai
            PromptEngine[Prompt Builder & JSON Parser]:::ai
            LLM[[🧠 Local LLM Engine<br/>e.g., GGUF Qwen 0.5B]]:::ai
        end
        
        %% Business Logic Layer
        subgraph Logic_Layer [Business Logic (Java MVVM)]
            direction TB
            VM[ViewModels]:::logic
            Repo[Data Repository]:::logic
            Statement[PDF Statement Generator]:::logic
        end
        
        %% Data Layer
        subgraph Data_Layer [Local Storage Layer]
            direction TB
            SQLC>SQLCipher Encryption]:::data
            Room[(Room Database<br/>SQLite)]:::data
        end
    end

    %% --- Connections & Data Flow ---
    
    %% Login Flow
    User -- Opens App --> Auth
    Auth -- Success --> Dashboard
    
    %% Navigation
    Dashboard --> ManualEntry
    Dashboard --> SmartEntry
    Dashboard --> ReceiptScan
    Dashboard --> Goals
    Dashboard -- Export --> Statement
    
    %% Manual Data Entry
    ManualEntry -- Validated Form Data --> VM
    
    %% Smart AI Flow
    SmartEntry -- "Natural Language Text" --> PromptEngine
    PromptEngine -- "Strict System Prompt + Text" --> LLM
    LLM -- "Raw JSON String" --> PromptEngine
    PromptEngine -- "Parsed Transaction Object" --> VM
    
    %% Receipt OCR Flow
    ReceiptScan -- "Image" --> OCR
    OCR -- "Extracted Text" --> PromptEngine
    
    %% Logic to Data Flow
    VM <--> Repo
    Repo <--> SQLC
    SQLC <--> Room
    
    %% Data to UI Flow (Observing Room via LiveData/Flow)
    Repo -. "Live Data Updates" .-> Dashboard
    Repo -. "Live Data Updates" .-> Goals
```

## How to Read the Diagram

*   **UI Layer (Orange)**: What the user interacts with. You'll use XML for the basic layout and Jetpack Compose for the dynamic elements like charts inside the Dashboard.
*   **AI Layer (Blue)**: The new intelligence core. Text or scanned receipt data is fed into a Prompt Builder. The Prompt Builder forces the Local LLM to output a clean JSON object, which is then parsed into a standard Java transaction object. **This all happens completely offline.**
*   **Logic Layer (Purple)**: Your Java ViewModels and Repository. They handle taking the inputs from the UI or AI layers and directing them to storage.
*   **Data Layer (Green)**: Your new Room Database. All data must pass through SQLCipher to be encrypted before being saved to the local SQLite file.
