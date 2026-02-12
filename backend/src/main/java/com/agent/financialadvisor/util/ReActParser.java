package com.agent.financialadvisor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for ReAct (Reasoning and Acting) pattern output from LLM
 */
public class ReActParser {

    private static final Logger log = LoggerFactory.getLogger(ReActParser.class);

    private static final Pattern THOUGHT_PATTERN = Pattern.compile("Thought:\\s*(.+?)(?=\\n(?:Action:|Final Answer:)|$)", Pattern.DOTALL);
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*(.+?)(?=\\n(?:Action Input:|Final Answer:)|$)", Pattern.DOTALL);
    private static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input:\\s*(.+?)(?=\\n(?:Observation:|Final Answer:)|$)", Pattern.DOTALL);
    private static final Pattern OBSERVATION_PATTERN = Pattern.compile("Observation:\\s*(.+?)(?=\\n(?:Thought:|Final Answer:)|$)", Pattern.DOTALL);
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile("Final Answer:\\s*(.+?)$", Pattern.DOTALL);

    public static ReActOutput parse(String response) {
        ReActOutput output = new ReActOutput();
        
        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(response);
        if (thoughtMatcher.find()) {
            output.thought = thoughtMatcher.group(1).trim();
        }
        
        Matcher actionMatcher = ACTION_PATTERN.matcher(response);
        if (actionMatcher.find()) {
            output.action = actionMatcher.group(1).trim();
        }
        
        Matcher actionInputMatcher = ACTION_INPUT_PATTERN.matcher(response);
        if (actionInputMatcher.find()) {
            output.actionInput = actionInputMatcher.group(1).trim();
        }
        
        Matcher observationMatcher = OBSERVATION_PATTERN.matcher(response);
        if (observationMatcher.find()) {
            output.observation = observationMatcher.group(1).trim();
        }
        
        Matcher finalAnswerMatcher = FINAL_ANSWER_PATTERN.matcher(response);
        if (finalAnswerMatcher.find()) {
            output.finalAnswer = finalAnswerMatcher.group(1).trim();
        }
        
        log.debug("Parsed ReAct output: thought={}, action={}, hasFinalAnswer={}", 
                output.thought != null, output.action != null, output.finalAnswer != null);
        
        return output;
    }

    public static class ReActOutput {
        public String thought;
        public String action;
        public String actionInput;
        public String observation;
        public String finalAnswer;

        public boolean hasAction() {
            return action != null && !action.trim().isEmpty();
        }

        public boolean hasFinalAnswer() {
            return finalAnswer != null && !finalAnswer.trim().isEmpty();
        }

        public String getThought() {
            return thought != null ? thought : "";
        }

        public String getAction() {
            return action != null ? action.trim() : null;
        }

        public String getActionInput() {
            return actionInput != null ? actionInput.trim() : null;
        }

        public String getObservation() {
            return observation != null ? observation : "";
        }

        public String getFinalAnswer() {
            return finalAnswer != null ? finalAnswer : "";
        }
    }
}

