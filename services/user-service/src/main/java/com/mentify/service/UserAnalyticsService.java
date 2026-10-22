package com.mentify.service;

import com.mentify.dto.UserRegistrationOverviewResponse;
import com.mentify.dto.UserAnalyticsOverviewResponse;

public interface UserAnalyticsService {

    UserRegistrationOverviewResponse getRegistrationOverview();

    UserAnalyticsOverviewResponse getAnalyticsOverview();
}
