package com.uhrynivcraft.model;

/**
 * Reasons a transaction can occur - stored in the "type" column of the transactions table.
 */
public enum TransactionType {
    MINING,
    MOB_KILL,
    FARMING,
    WOODCUTTING,
    FISHING,
    PAY_SEND,
    PAY_RECEIVE,
    ADMIN_GIVE,
    ADMIN_TAKE,
    SHOP_PURCHASE,
    QUEST_REWARD,
    OTHER
}
