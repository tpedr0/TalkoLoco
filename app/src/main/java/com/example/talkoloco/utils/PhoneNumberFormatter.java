package com.example.talkoloco.utils;

/**
 * the PhoneNumberFormatter class provides utility methods for formatting and validating phone numbers.
 */
public class PhoneNumberFormatter {
    private static final String COUNTRY_CODE = "+1 ";

    /**
     * formats the given phone number input into the desired format: "+1 (XXX) XXX-XXXX".
     *
     * @param input the phone number input to be formatted
     * @return the formatted phone number
     */
    public static String format(String input) {
        // removes all non-digit characters
        String numbers = input.replaceAll("[^\\d]", "");

        // initializes StringBuilder with country code
        StringBuilder formatted = new StringBuilder(COUNTRY_CODE);

        // if empty after cleaning, just return country code
        if (numbers.isEmpty()) {
            return formatted.toString();
        }

        // remove country code if present at the start
        if (numbers.startsWith("1")) {
            numbers = numbers.substring(1);
        }

        // format: +1 (XXX) XXX-XXXX
        int length = numbers.length();

        // add area code with parentheses
        if (length > 0) {
            formatted.append("(");
            formatted.append(numbers.substring(0, Math.min(3, length)));
            formatted.append(")");

            // add space and first three digits
            if (length > 3) {
                formatted.append(" ");
                formatted.append(numbers.substring(3, Math.min(6, length)));

                // add hyphen and last four digits
                if (length > 6) {
                    formatted.append("-");
                    formatted.append(numbers.substring(6, Math.min(10, length)));
                }
            }
        }

        return formatted.toString();
    }

    /**
     * strips the formatting from the given phone number.
     *
     * @param formattedNumber the formatted phone number
     * @return the phone number without formatting
     */
    public static String stripFormatting(String formattedNumber) {
        return formattedNumber.replaceAll("[^\\d+]", "");
    }

    /**
     * checks if the given phone number is valid.
     *
     * @param phoneNumber the phone number to be validated
     * @return true if the phone number is valid, false otherwise
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        String stripped = stripFormatting(phoneNumber);
        // checks if it's exactly 11 digits (including country code)
        return stripped.length() == 11 && stripped.startsWith("+1");
    }

    /**
     * ensures that the given phone number has the country code prefix.
     *
     * @param phoneNumber the phone number to be checked
     * @return the phone number with the country code prefix
     */
    public static String ensureCountryCode(String phoneNumber) {
        String stripped = stripFormatting(phoneNumber);
        if (!stripped.startsWith("+")) {
            if (stripped.startsWith("1")) {
                return "+" + stripped;
            } else {
                return "+1" + stripped;
            }
        }
        return stripped;
    }
}