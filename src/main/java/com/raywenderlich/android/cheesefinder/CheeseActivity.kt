/*
 * Copyright (c) 2019 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.cheesefinder

import android.text.Editable
import android.text.TextWatcher
import io.reactivex.BackpressureStrategy.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_cheeses.*

class CheeseActivity : BaseSearchActivity() {

    private lateinit var disposable: Disposable

    private fun createButtonClickObservable(): Observable<String> {
        return Observable.create { emitter ->
            searchButton.setOnClickListener {
                emitter.onNext(queryEditText.text.toString())
            }

            emitter.setCancellable {
                searchButton.setOnClickListener(null)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val buttonClickStream = createButtonClickObservable().toFlowable(LATEST)
        val textChangeStream = createTextChangeObservable().toFlowable(BUFFER)
        val searchTextObservable = Flowable.merge<String>(buttonClickStream, textChangeStream)
        disposable = searchTextObservable
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { showProgress() }
            .observeOn(Schedulers.io())
            .map { cheeseSearchEngine.search(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                hideProgress()
                showResult(it)
        }
    }

    private fun createTextChangeObservable(): Observable<String> {
        val textChangeObservable = Observable.create<String> { emitter ->
            val textWatcher = object: TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

                override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {
                    emitter.onNext(p0.toString())
                }

                override fun afterTextChanged(p0: Editable?) = Unit

            }

            queryEditText.addTextChangedListener(textWatcher)
            emitter.setCancellable { queryEditText.removeTextChangedListener(textWatcher) }

        }
        return textChangeObservable.filter {
            it.length >= 2
        }
    }

    override fun onStop() {
        super.onStop()
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
    }
}