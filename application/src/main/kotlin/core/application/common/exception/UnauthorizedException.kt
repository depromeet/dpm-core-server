package core.application.common.exception

class UnauthorizedException :
    BusinessException(
        GlobalExceptionCode.UNAUTHORIZED,
    )
