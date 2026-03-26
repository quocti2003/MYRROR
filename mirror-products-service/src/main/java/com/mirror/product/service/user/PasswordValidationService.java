package com.mirror.product.service.user;

import com.mirror.product.exception.user.WeakPasswordException;
import lombok.extern.slf4j.Slf4j;
import org.passay.*;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class PasswordValidationService {
    
    private final PasswordValidator passwordValidator;
    
    public PasswordValidationService() {
        this.passwordValidator = new PasswordValidator(Arrays.asList(
            new LengthRule(8, 128),
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            new CharacterRule(EnglishCharacterData.LowerCase, 1),
            new CharacterRule(EnglishCharacterData.Digit, 1),
            new CharacterRule(EnglishCharacterData.Special, 1),
            new WhitespaceRule(),
            new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 5, false),
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 5, false),
            new IllegalSequenceRule(EnglishSequenceData.USQwerty, 5, false),
            new RepeatCharacterRegexRule(4)
        ));
    }
    
    public void validatePassword(String password) {
        RuleResult result = passwordValidator.validate(new PasswordData(password));
        
        if (!result.isValid()) {
            String errorMessage = String.join(", ", passwordValidator.getMessages(result));
            log.warn("Password validation failed: {}", errorMessage);
            throw new WeakPasswordException("Password does not meet security requirements: " + errorMessage);
        }
        
        if (isCommonPassword(password)) {
            throw new WeakPasswordException("Password is too common and easily guessable");
        }
    }
    
    private boolean isCommonPassword(String password) {
        String[] commonPasswords = {
            "password", "123456", "password123", "admin", "qwerty", 
            "letmein", "welcome", "monkey", "1234567890", "abc123"
        };
        
        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.contains(common)) {
                return true;
            }
        }
        return false;
    }
}