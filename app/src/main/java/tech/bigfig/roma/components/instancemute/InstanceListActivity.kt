package tech.bigfig.roma.components.instancemute

import android.os.Bundle
import android.view.MenuItem
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.android.synthetic.main.toolbar_basic.*
import tech.bigfig.roma.BaseActivity
import tech.bigfig.roma.R
import tech.bigfig.roma.components.instancemute.fragment.InstanceListFragment
import javax.inject.Inject

class InstanceListActivity: BaseActivity(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector() = androidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_list)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setTitle(R.string.title_domain_mutes)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, InstanceListFragment())
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}