package com.spring.ruleengine.service;

import com.spring.ruleengine.exception.InvalidDataFormatException;
import com.spring.ruleengine.exception.InvalidRuleFormatException;
import com.spring.ruleengine.exception.KeyNotFoundException;
import com.spring.ruleengine.exception.MissingAttributeException;
import com.spring.ruleengine.model.ASTNode;
import com.spring.ruleengine.model.RuleEntity;
import com.spring.ruleengine.repository.RuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RuleService {

    @Autowired
    private RuleRepository ruleRepository;

    private static final Set<String> attributeCatalog = new HashSet<>(
            Set.of(
                    "age", "department", "income", "spend", "salary", "experience",
                    "position", "location", "education", "maritalStatus", "gender",
                    "employmentType", "creditScore", "loanAmount", "loanType",
                    "employmentStatus", "children", "dependentCount", "vehicleType",
                    "propertyOwnership", "annualIncome", "monthlyExpense", "savings",
                    "taxBracket", "healthInsurance", "lifeInsurance", "disabilityInsurance",
                    "investments", "retirementFund", "bonus", "jobLevel",
                    "promotionHistory", "projectCount", "certifications", "languages",
                    "workHoursPerWeek", "vacationDays", "performanceRating",
                    "disciplinaryActions", "companyTenure", "contractLength",
                    "jobSatisfaction", "customerFeedback", "revenueGenerated",
                    "salesTargetAchieved", "attendance", "remoteWorkPercentage",
                    "overtimeHours", "stockOptions", "volunteerHours", "innovationScore",
                    "trainingHours", "commuteTime", "teamSize", "managerFeedback",
                    "careerProgression", "leadershipPotential", "skillsAssessment",
                    "projectDeadlineMet", "clientSatisfaction", "stressLevel",
                    "riskTolerance", "financialLiabilities", "mortgagePayments",
                    "rentPayments", "utilityExpenses", "childcareCosts",
                    "tuitionFees", "otherDebts", "partnerIncome", "familySupport",
                    "investmentReturns", "annualBonus", "commissionEarned",
                    "travelExpenses", "housingAllowance", "relocationAllowance",
                    "internetUsage", "digitalSkills", "socialMediaEngagement",
                    "networkingEventsAttended", "professionalAffiliations",
                    "workplaceAccidents", "wellnessProgramParticipation",
                    "mentalHealthSupport", "physicalActivityLevel", "dietaryHabits",
                    "smokingStatus", "alcoholConsumption", "legalIssues",
                    "immigrationStatus", "visaType", "citizenshipStatus",
                    "retirementEligibility", "pensionPlan", "profitSharing",
                    "severancePay", "unemploymentBenefits", "grade"
            )
    );


    // Validates the rule string
    public void validateRule(String rule) throws InvalidRuleFormatException {
        if (rule == null || rule.isEmpty()) {
            throw new InvalidRuleFormatException("Rule cannot be empty.");
        }

        // Check if the rule contains an operator
        if (!rule.contains("==") && !rule.contains(">") && !rule.contains("<")) {
            throw new InvalidRuleFormatException("Missing comparison operator.");
        }
    }

    // Validates the data format, expects numeric values in this example
    public void validateDataFormat(String data) throws InvalidDataFormatException {
        try {
            // Attempt to parse numeric values
            Integer.parseInt(data);
        } catch (NumberFormatException e) {
            throw new InvalidDataFormatException("Data format is invalid, should be a number.");
        }
    }


    // Validates if the attribute exists in the catalog
    public void validateAttribute(String attribute) throws InvalidRuleFormatException {
        if (!attributeCatalog.contains(attribute)) {
            throw new InvalidRuleFormatException("Attribute '" + attribute + "' is not part of the catalog.");
        }
    }

    public ASTNode createRule(String ruleString) {
        if (ruleString == null || ruleString.trim().isEmpty()) {
            throw new InvalidRuleFormatException("Rule string cannot be null or empty.");
        }

        try {
            return parseRuleToAST(ruleString);
        } catch (Exception e) {
            throw new InvalidRuleFormatException("Invalid rule format: " + ruleString);
        }
    }

    public ASTNode combineRules(List<String> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new InvalidRuleFormatException("Rule list cannot be null or empty.");
        }

        List<ASTNode> ruleASTs = rules.stream()
                .map(this::createRule)
                .collect(Collectors.toList());

        long andCount = rules.stream().filter(r -> r.toUpperCase().contains("AND")).count();
        long orCount = rules.stream().filter(r -> r.toUpperCase().contains("OR")).count();
        String dominantOperator = andCount >= orCount ? "AND" : "OR";

        ASTNode combinedAST = new ASTNode(dominantOperator);
        for (ASTNode ruleAST : ruleASTs) {
            combinedAST.addChild(ruleAST);
        }

        return combinedAST;
    }

    public boolean evaluateRule(ASTNode node, Map<String, Object> data) {
        if (node == null || data == null) {
            throw new InvalidRuleFormatException("Node and data must not be null.");
        }

        try {
            return evaluate(node, data);
        } catch (KeyNotFoundException e) {
            throw new KeyNotFoundException("Key not found in data: " + e.getMessage());
        }
    }

    private ASTNode parseRuleToAST(String ruleString) {
        ruleString = ruleString.trim();

        // Remove surrounding parentheses
        while (ruleString.startsWith("(") && ruleString.endsWith(")")) {
            ruleString = ruleString.substring(1, ruleString.length() - 1).trim();
        }

        Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < ruleString.length(); i++) {
            char ch = ruleString.charAt(i);
            if (ch == '(') {
                stack.push(i);
            } else if (ch == ')') {
                if (!stack.isEmpty()) stack.pop();
            } else if ((ch == 'A' || ch == 'O') && stack.isEmpty()) {
                String operator = ch == 'A' ? "AND" : "OR";
                String[] parts = splitOnFirst(ruleString.substring(0, i), operator);
                ASTNode parentNode = new ASTNode(operator);
                parentNode.addChild(parseRuleToAST(parts[0].trim()));
                parentNode.addChild(parseRuleToAST(ruleString.substring(i + operator.length()).trim()));
                return parentNode;
            }
        }

        // Handle remaining OR operations
        if (ruleString.contains(" OR ")) {
            String[] operands = ruleString.split(" OR ");
            ASTNode orNode = new ASTNode("OR");
            for (String operand : operands) {
                orNode.addChild(parseRuleToAST(operand.trim()));
            }
            return orNode;
        }

        // Create a node for a single operand
        return new ASTNode("operand", ruleString.replaceAll("^\\(|\\)$", "").trim());
    }

    private boolean evaluate(ASTNode node, Map<String, Object> data) {
        switch (node.getType()) {
            case "AND":
                // Evaluate all children in AND node
                return node.getChildren().stream().allMatch(child -> evaluate(child, data));

            case "OR":
                // Evaluate children in OR node; return true if any child is true
                return node.getChildren().stream().anyMatch(child -> evaluate(child, data));

            case "operand":
                return evaluateOperand(node.getValue(), data);

            default:
                throw new InvalidRuleFormatException("Unknown node type: " + node.getType());
        }
    }

    private boolean evaluateOperand(String operand, Map<String, Object> data) {
        String[] parts = operand.split(" ");
        if (parts.length != 3) {
            throw new InvalidRuleFormatException("Invalid operand format: " + operand);
        }

        String attribute = parts[0].trim();
        String operator = parts[1].trim();
        String value = parts[2].trim();

        // Check for missing attribute before accessing data
        if (!data.containsKey(attribute)) {
            throw new MissingAttributeException("Required field missing: " + attribute);
        }

        Object dataValue = data.get(attribute);
        return compareValues(dataValue, operator, value);
    }

    private boolean compareValues(Object dataValue, String operator, String value) {
        if (dataValue instanceof String && value.startsWith("'") && value.endsWith("'")) {
            String trimmedValue = value.substring(1, value.length() - 1);
            return dataValue.toString().equals(trimmedValue);
        }

        try {
            if (dataValue instanceof Double || value.contains(".")) {
                double doubleDataValue = Double.parseDouble(dataValue.toString());
                double doubleValue = Double.parseDouble(value);
                switch (operator) {
                    case ">":
                        return doubleDataValue > doubleValue;
                    case "<":
                        return doubleDataValue < doubleValue;
                    case "=":
                        return doubleDataValue == doubleValue;
                    case "!=":
                        return doubleDataValue != doubleValue;
                    case ">=":
                        return doubleDataValue >= doubleValue;
                    case "<=":
                        return doubleDataValue <= doubleValue;
                    default:
                        throw new InvalidRuleFormatException("Unknown operator: " + operator);
                }
            }

            // Integer comparison remains the same
            int intDataValue = Integer.parseInt(dataValue.toString());
            int intValue = Integer.parseInt(value);
            switch (operator) {
                case ">":
                    return intDataValue > intValue;
                case "<":
                    return intDataValue < intValue;
                case "=":
                    return intDataValue == intValue;
                case "!=":
                    return intDataValue != intValue;
                case ">=":
                    return intDataValue >= intValue;
                case "<=":
                    return intDataValue <= intValue;
                default:
                    throw new InvalidRuleFormatException("Unknown operator: " + operator);
            }

        } catch (NumberFormatException e) {
            // Handle invalid number format exception
            throw new InvalidDataFormatException("Invalid numeric format for comparison: " + value);
        }
    }

    private String[] splitOnFirst(String str, String delimiter) {
        int index = str.indexOf(delimiter);
        if (index == -1) return new String[]{str};
        return new String[]{str.substring(0, index).trim(), str.substring(index + delimiter.length()).trim()};
    }

    public RuleEntity saveRule(String ruleString) {
        RuleEntity ruleEntity = new RuleEntity();
        ruleEntity.setRuleString(ruleString);
        return ruleRepository.save(ruleEntity);
    }
}
