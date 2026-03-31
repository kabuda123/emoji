package com.company.emoji.user;

import com.company.emoji.user.dto.DeleteAccountRequest;
import com.company.emoji.user.dto.DeleteAccountResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AccountService {

    public DeleteAccountResponse requestDeletion(String userId, DeleteAccountRequest request) {
        return new DeleteAccountResponse("SCHEDULED", Instant.now().plus(7, ChronoUnit.DAYS));
    }
}
