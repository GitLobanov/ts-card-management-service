package by.lobanov.cardmanagementservice.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ServiceMessagesUtil {

    public static final String USER_NOT_FOUND_WITH_ID = "User not found with id: ";
    public static final String EMAIL_IS_ALREADY_TAKEN = "Email %s is already taken!";
    public static final String USER_NOT_FOUND_WITH_EMAIL = "User not found with email: ";
    public static final String YOU_CANNOT_DELETE_YOUR_OWN_ACCOUNT = "You cannot delete your own account.";
    public static final String CANNOT_DELETE_PROTECTED_USER = "Cannot delete protected user: ";
    public static final String ROLE_NOT_FOUND = "Role not found: ";
    public static final String INVALID_ROLE_NAME_PROVIDED = "Invalid role name provided: ";


    public static final String ADMIN_CANNOT_MANUALLY_SET_STATUS_EXPIRED = "Admin cannot manually set status to EXPIRED.";
    public static final String CANNOT_ACTIVATE_EXPIRED_CARD_ID = "Cannot activate an expired card ID: ";
    public static final String CARD_NOT_FOUND = "Card not found with id: ";


    public static final String REFRESH_TOKEN_WAS_EXPIRED = "Refresh token was expired. Please make a new signin request";
    public static final String COULD_NOT_GENERATE_UNIQUE_CARD_NUMBER = "Could not generate a unique card number.";
    public static final String INPUT_NUMBER_CANNOT_BE_NULL_OR_EMPTY = "Input number cannot be null or empty";
    public static final String FAILED_TO_CALCULATE_DIGIT_FOR_INPUT = "Failed to calculate check digit for input: ";
    public static final String ENCRYPTION_HAS_NOT_BEEN_INJECTED_INTO_CARD_NUMBER_CONVERTER = "EncryptionService has not been injected into CardNumberConverter";
}
