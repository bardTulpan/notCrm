package com.pipeline.crm.avatar;

import java.util.UUID;

public record AvatarVersion(UUID studentId, String imageHash) {
}
