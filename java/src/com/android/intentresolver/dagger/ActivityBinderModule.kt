package com.android.intentresolver.dagger

import android.app.Activity
import com.android.intentresolver.ChooserActivity
import com.android.intentresolver.IntentForwarderActivity
import com.android.intentresolver.ResolverActivity
import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

/** Injection instructions for injectable [Activities][Activity]. */
@Module
interface ActivityBinderModule {

    @Binds
    @IntoMap
    @ClassKey(ChooserActivity::class)
    @ActivityScope
    fun bindChooserActivity(activity: ChooserActivity): Activity

    @Binds
    @IntoMap
    @ClassKey(ResolverActivity::class)
    @ActivityScope
    fun bindResolverActivity(activity: ResolverActivity): Activity

    @Binds
    @IntoMap
    @ClassKey(IntentForwarderActivity::class)
    @ActivityScope
    fun bindIntentForwarderActivity(activity: IntentForwarderActivity): Activity
}