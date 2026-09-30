package com.maxeydev.picklelog.domain.backup

enum class ImportProblem {
    NOT_READABLE,
    NOT_A_PICKLELOG_EXPORT,
    NEWER_VERSION,
    CORRUPT_CONTENT,
    TOO_LARGE,
    WRITE_FAILED,
}
