package com.feedrelay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FeedrelayApplication

fun main(args: Array<String>) {
	runApplication<FeedrelayApplication>(*args)
}
