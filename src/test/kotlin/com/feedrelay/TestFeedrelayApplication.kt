package com.feedrelay

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<FeedrelayApplication>().with(TestcontainersConfiguration::class).run(*args)
}
