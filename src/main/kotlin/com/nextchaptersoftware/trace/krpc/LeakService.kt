package com.nextchaptersoftware.trace.krpc

import io.ktor.server.application.ApplicationCall
import kotlinx.rpc.annotations.Rpc

@Rpc
interface LeakService {
    suspend fun echo(value: String): String
}

class LeakServiceImpl(
    private val call: ApplicationCall,
    private val leakProbe: LeakProbe
) : LeakService {
    override suspend fun echo(value: String): String {
        leakProbe.track(call, this)
        require(call.request.headers["TestHeader"] == "test-header") {
            "Expected TestHeader"
        }
        return value
    }
}
