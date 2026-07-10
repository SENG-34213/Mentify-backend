package com.mentify.userservice.service;

public interface AdminService {

    /**
     * Updates the active status of a user.
     *
     * @param userId the ID of the user to update
     * @param active the new active status
     * @throws com.mentify.userservice.exception.UserNotFoundException if no user exists with the given ID
     */
    void updateUserStatus(Long userId, boolean active);
}
