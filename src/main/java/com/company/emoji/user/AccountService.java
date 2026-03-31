package com.company.emoji.user;

import com.company.emoji.user.dto.DeleteAccountRequest;
import com.company.emoji.user.dto.DeleteAccountResponse;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final UserAccountService userAccountService;

    public AccountService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public DeleteAccountResponse requestDeletion(String userId, DeleteAccountRequest request) {
        return userAccountService.requestDeletion(userId, request);
    }
}
