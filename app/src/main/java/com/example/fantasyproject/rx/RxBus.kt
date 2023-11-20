package com.example.fantasyproject.rx

import android.database.Observable
import rx.subjects.PublishSubject

object RxBus {

    private var publisher = PublishSubject.create<Any>()

    fun publish(event: Any){
        publisher.onNext(event)
    }

    fun <T> listen(eventType: Class<T>): rx.Observable<T>? = publisher.ofType(eventType)
}