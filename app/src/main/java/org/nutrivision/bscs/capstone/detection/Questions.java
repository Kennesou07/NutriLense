package org.nutrivision.bscs.capstone.detection;

import java.util.Arrays;

public class Questions {
    private static final String[] questions = {
            "Have you been diagnosed with diabetes?",
            "Have you been diagnosed with high blood pressure?",
            "Have you been diagnosed with heart disease?",
            "Have you been diagnosed with obesity?",
            "Have you been diagnosed with kidney disease?"
    };
    private static final String[] choices = {
            "Yes",
            "No"
    };
    public static int currentQuestionIndex;
    private boolean[] isQuestionAnswered;

    public Questions() {
        currentQuestionIndex = 0;
        isQuestionAnswered = new boolean[questions.length];
        Arrays.fill(isQuestionAnswered, false);
    }

    public String getCurrentQuestion() {
        return questions[currentQuestionIndex];
    }

    public String[] getChoices() {
        return choices;
    }

    public void moveToNextQuestion() {
        if (currentQuestionIndex < questions.length - 1) {
            currentQuestionIndex++;
        }
    }

    public boolean isLastQuestion() {
        return currentQuestionIndex == questions.length - 1;
    }
    public boolean isQuestionDone() {
        return isQuestionAnswered[currentQuestionIndex];
    }

    public void setQuestionAnswered(boolean answered) {
        isQuestionAnswered[currentQuestionIndex] = answered;
    }

    public boolean areAllQuestionsAnswered() {
        for (boolean answered : isQuestionAnswered) {
            if (!answered) {
                return false; // If any question is not answered, return false
            }
        }
        return true; // All questions are answered
    }

    public static int getQuestionSize() {
        return questions.length;
    }
}
