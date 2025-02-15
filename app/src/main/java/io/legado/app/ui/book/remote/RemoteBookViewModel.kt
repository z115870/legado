package io.legado.app.ui.book.remote

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.model.localBook.LocalBook
import io.legado.app.ui.book.remote.manager.RemoteBookWebDav
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.util.*

class RemoteBookViewModel(application: Application): BaseViewModel(application){

    val dirList = arrayListOf<RemoteBook>()

    var dataCallback: DataCallback? = null

    val dataFlow = callbackFlow<List<RemoteBook>> {

        val list = Collections.synchronizedList(ArrayList<RemoteBook>())

        dataCallback = object : DataCallback {

            override fun setItems(remoteFiles: List<RemoteBook>) {
                list.clear()
                list.addAll(remoteFiles)
                trySend(list)
            }

            override fun addItems(remoteFiles: List<RemoteBook>) {
                list.addAll(remoteFiles)
                trySend(list)
            }

            override fun clear() {
                list.clear()
                trySend(emptyList())
            }
        }

        awaitClose {
            dataCallback = null
        }
    }.flowOn(Dispatchers.IO)

    init {
        execute {
            RemoteBookWebDav.initRemoteContext()
        }
    }

    fun loadRemoteBookList(path: String) {
        execute {
            dataCallback?.clear()
            val bookList = RemoteBookWebDav.getRemoteBookList(path)
            dataCallback?.setItems(bookList)
        }.onError {
            AppLog.put("获取webDav书籍出错\n${it.localizedMessage}", it)
            context.toastOnUi("获取webDav书籍出错\n${it.localizedMessage}")
        }
    }

    fun addToBookshelf(remoteBooks: HashSet<RemoteBook>, finally: () -> Unit) {
        execute {
            remoteBooks.forEach { remoteBook ->
                val downloadBookPath = RemoteBookWebDav.getRemoteBook(remoteBook)
                downloadBookPath?.let {
                    LocalBook.importFile(it)
                    remoteBook.isOnBookShelf = true
                }
            }
        }.onError {
            AppLog.put("导入出错\n${it.localizedMessage}", it)
            context.toastOnUi("导入出错\n${it.localizedMessage}")
        }.onFinally {
            finally.invoke()
        }
    }

    interface DataCallback {

        fun setItems(remoteFiles: List<RemoteBook>)

        fun addItems(remoteFiles: List<RemoteBook>)

        fun clear()

    }
}