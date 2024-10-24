package com.spring.ruleengine.controller;

import com.spring.ruleengine.exception.InvalidDataFormatException;
import com.spring.ruleengine.exception.InvalidRuleFormatException;
import com.spring.ruleengine.exception.MissingAttributeException;
import com.spring.ruleengine.service.RuleService;
import com.spring.ruleengine.model.ASTNode;
import com.spring.ruleengine.model.RuleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    @Autowired
    private RuleService ruleService;

    // Exception handler for InvalidRuleFormatException
    @ExceptionHandler(InvalidRuleFormatException.class)
    public ResponseEntity<String> handleInvalidRuleFormatException(InvalidRuleFormatException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // Exception handler for InvalidDataFormatException
    @ExceptionHandler(InvalidDataFormatException.class)
    public ResponseEntity<String> handleInvalidDataFormatException(InvalidDataFormatException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // Exception handler for MissingAttributeException
    @ExceptionHandler(MissingAttributeException.class)
    public ResponseEntity<String> handleMissingAttribute(MissingAttributeException ex) {
        return ResponseEntity.badRequest().body("There was a problem with your input: " + ex.getMessage());
    }

    // Endpoint to create a rule
    @PostMapping("/create")
    public ResponseEntity<?> createRule(@RequestBody Map<String, String> requestBody) {
        String ruleString = requestBody.get("ruleString");

        // Validate the ruleString
        if (ruleString == null || ruleString.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Please enter a rule string.");
        }

        // Validate the rule format using RuleService
        try {
            ruleService.validateRule(ruleString);
        } catch (InvalidRuleFormatException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }


        // Create the rule (ASTNode representation)
        ASTNode ruleNode = ruleService.createRule(ruleString);
        return ResponseEntity.ok(ruleNode);
    }




    // Endpoint to combine multiple rules
    @PostMapping("/combine")
    public ResponseEntity<?> combineRules(@RequestBody Map<String, List<String>> request) {
        List<String> rules = request.get("rules");

        // Validate the rules input
        if (rules == null || rules.isEmpty() || rules.stream().allMatch(String::isEmpty)) {
            return ResponseEntity.badRequest().body("Please enter at least one rule.");
        }
        try {
            ruleService.validateRule(String.valueOf(rules));
        } catch (InvalidRuleFormatException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        // Check for duplicate rules
        List<String> duplicateRules = findDuplicateRules(rules);
        if (!duplicateRules.isEmpty()) {
            return ResponseEntity.badRequest().body("Duplicate rules detected: " + String.join(", ", duplicateRules));
        }


        // Combine the rules into one ASTNode
        ASTNode combinedRule = ruleService.combineRules(rules);
        return ResponseEntity.ok(combinedRule);
    }


    // Endpoint to evaluate a rule
    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluateRule(@RequestBody Map<String, Object> request) {
        String ruleString = (String) request.get("ruleString");
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        // Validate the ruleString
        if (ruleString == null || ruleString.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Please enter a rule string.");
        }

        // Validate the data for evaluation
        if (data == null || data.isEmpty()) {
            return ResponseEntity.badRequest().body("Please provide data for evaluation.");
        }

        // Validate the rule format using RuleService
        try {
            ruleService.validateRule(ruleString);
        } catch (InvalidRuleFormatException e) {
            return ResponseEntity.badRequest().body("Invalid rule format: " + e.getMessage());
        }

        // Validate each attribute in the rule
        for (String key : data.keySet()) {
            try {
                ruleService.validateAttribute(key);  // Check if the attribute exists in the catalog
            } catch (InvalidRuleFormatException e) {
                return ResponseEntity.badRequest().body("Invalid attribute in rule: " + key);
            }
        }

        // Check if the data contains all necessary fields for the rule
        List<String> missingFields = findMissingFields(ruleString, data);
        if (!missingFields.isEmpty()) {
            return ResponseEntity.badRequest().body("Rule references non-existing field(s): " + String.join(", ", missingFields));
        }

        // Evaluate the rule
        boolean result;
        try {
            result = ruleService.evaluateRule(ruleService.createRule(ruleString), data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Evaluation error: " + e.getMessage());
        }

        // Return the result of the evaluation
        return ResponseEntity.ok(result);
    }

    @PostMapping("/save")
    public ResponseEntity<RuleEntity> saveRule(@RequestBody String ruleString) {
        RuleEntity savedRule = ruleService.saveRule(ruleString);
        return ResponseEntity.ok(savedRule);
    }


    private boolean isValidRule(String ruleString) {
        // Improved regex pattern for rule validation
        String rulePattern = "^\\s*(\\(.*\\)|[a-zA-Z_][a-zA-Z0-9_]*\\s*(>|<|>=|<=|=|!=)\\s*(?:'[^']*'|\\d+))\\s*(" +
                "\\s*(AND|OR)\\s*(\\(.*\\)|[a-zA-Z_][a-zA-Z0-9_]*\\s*(>|<|>=|<=|=|!=)\\s*(?:'[^']*'|\\d+)))*\\s*$";
        return ruleString.matches(rulePattern);
    }

    private List<String> findDuplicateRules(List<String> rules) {
        // Use a set to track duplicates and return a list of duplicate rules
        HashSet<String> uniqueRules = new HashSet<>();
        HashSet<String> duplicates = new HashSet<>();

        for (String rule : rules) {
            if (!uniqueRules.add(rule.trim())) {
                duplicates.add(rule.trim());
            }
        }
        return duplicates.stream().toList(); // Return list of duplicate rules
    }

    private List<String> findMissingFields(String ruleString, Map<String, Object> data) {
        // Use a regex to capture the variable names correctly
        String regex = "([a-zA-Z_][a-zA-Z0-9_]*)\\s*(>|<|>=|<=|=|!=)\\s*(['\"]?)([\\w\\s]+)(['\"]?)"; // Updated to handle string values
        Matcher matcher = Pattern.compile(regex).matcher(ruleString);
        HashSet<String> missingFields = new HashSet<>();

        while (matcher.find()) {
            String field = matcher.group(1); // Capture the field (first group)
            if (!data.containsKey(field)) {
                missingFields.add(field); // Collect missing fields
            }
        }
        return missingFields.stream().toList(); // Return list of missing fields
    }
}
