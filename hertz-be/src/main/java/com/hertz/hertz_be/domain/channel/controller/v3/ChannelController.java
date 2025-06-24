package com.hertz.hertz_be.domain.channel.controller.v3;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("channelControllerV3")
@RequestMapping("/api/v3")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@Tag(name = "튜닝/채널 관련 v3 API")
public class ChannelController {
}
