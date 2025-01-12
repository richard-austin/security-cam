package com.securitycam.commands


import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull


class GetSnapshotCommand {
    @NotBlank
    @NotNull
    //TODO: Find better url verification regex than this, and the one used for rtsp url verification
//    @Pattern(regexp="(/\\b((http):\\/\\/[-\\w]+(\\.\\w[-\\w]*)+|(?:[a-z0-9](?:[-a-z0-9]*[a-z0-9])?\\.)+(?: com\\b|edu\\b|biz\\b|gov\\b|in(?:t|fo)\\b|mil\\b|net\\b|org\\b|[a-z][a-z]\\b))(\\\\:\\d+)?(\\/[^.!,?;\\\"'<>()\\[\\]{}\\s\\x7F-\\xFF]*(?:[.!,?]+[^.!,?;\\\"'<>()\\[\\]{}\\s\\x7F-\\xFF]+)*)?/")
    String url

    String cred
}
