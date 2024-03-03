package security.cam.commands

import grails.validation.Validateable

class GetSnapshotCommand implements Validateable{
    String url
    String cred
    static constraints = {
        url(blank: false,
            nullable: false,
            validator: { url ->
                //TODO: Find better url verification regex than this, and the one used for rtsp url verification
//                if (!url.matches(/\b((http):\/\/[-\w]+(\.\w[-\w]*)+|(?:[a-z0-9](?:[-a-z0-9]*[a-z0-9])?\.)+(?: com\b|edu\b|biz\b|gov\b|in(?:t|fo)\b|mil\b|net\b|org\b|[a-z][a-z]\b))(\\:\d+)?(\/[^.!,?;\"'<>()\[\]{}\s\x7F-\xFF]*(?:[.!,?]+[^.!,?;\"'<>()\[\]{}\s\x7F-\xFF]+)*)?/))
//                    return "Invalid url ${url}"
                return
            })
        cred(nullable: false)
    }
}
