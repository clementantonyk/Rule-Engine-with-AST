# Rule Engine with Abstrat Syntax Tree

## Overview
The Rule Engine with AST is designed to dynamically create, modify, and evaluate rules for determining user eligibility based on specific attributes like age, department, income, and spend. The system uses an Abstract Syntax Tree (AST) to represent conditional rules and allows for efficient combination and modification of rules.

## Features

- Dynamically creates rules as Abstract Syntax Trees (ASTs) from user input.
- Combines multiple rules into a single optimized AST for evaluation.
- Evaluates rules against user attributes and returns eligibility based on the rule logic.
- Includes error handling for invalid rule strings or data formats.
- Incorporates validations for attributes using a predefined catalog.

## Requirements
- **IDE**: IntelliJ IDEA Ultimate or Visual Studio Code (or any other preferred code editor)

## Installation

**Make sure that IDE is Installed and Opened**

 1. Clone the repository by using the command
     
     ```
      git clone https://github.com/clementantonyk/Rule-Engine-with-AST.git
     ```
     
 2. Navigate to the project directory:
    
    ```
    cd Rule-Engine-with-AST
    ```
3. Run ```RuleEngineApplication.java```

## Usage

**After running ```RuleEngineApplication.java```, you can use the following URLs**:

1. To navigate to the Rule Engine Dashboard
   
   ```
   http://localhost:8080
   ```

## Screenshot

![image](https://github.com/user-attachments/assets/0891e684-c2db-4927-827a-9a8ae6069c2b)

**Rule Creation Object**

![image](https://github.com/user-attachments/assets/ced21419-92e6-4e46-934e-aa09c5ab5c99)

**Combining a list of rule strings into a single AST, optimizing efficiency by minimizing redundant checks and leveraging a most frequent operator heuristic**

![image](https://github.com/user-attachments/assets/43ca2496-b14e-44b7-b181-49f64d2ce853)

