package com.jobskillsmatcher.auditlog.model;

public enum LogAction {
    HTTP_MUTATION,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    REGISTER,
    TOKEN_REFRESH,
    SYSTEM_EVENT
}
