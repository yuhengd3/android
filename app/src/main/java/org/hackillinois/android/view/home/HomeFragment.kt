package org.hackillinois.android.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import org.hackillinois.android.R
import org.hackillinois.android.common.TimeInfo
import org.hackillinois.android.database.entity.Event
import org.hackillinois.android.view.eventinfo.EventInfoFragment
import org.hackillinois.android.view.MainActivity
import org.hackillinois.android.view.custom.CustomRefreshView
import org.hackillinois.android.view.home.eventlist.EventClickListener
import org.hackillinois.android.view.home.eventlist.EventsSection
import org.hackillinois.android.viewmodel.HomeViewModel

class HomeFragment : Fragment(), CountdownManager.CountDownListener, EventClickListener {

    private lateinit var eventsListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private lateinit var ongoingEventsSection: EventsSection
    private lateinit var upcomingEventsSection: EventsSection

    private lateinit var viewModel: HomeViewModel
    private val countDownManager = CountdownManager(this)

    private var isActive = false

    private val refreshIconSize = 100

    private val numberOfUpcomingEvents = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context?.let {
            val ongoingHeaderColor = ContextCompat.getColor(it, R.color.alternateTextColor)
            ongoingEventsSection = EventsSection(
                mutableListOf(),
                "ONGOING",
                ongoingHeaderColor,
                false,
                this,
                it
            )
            val upcomingHeaderColor = ContextCompat.getColor(it, R.color.primaryTextColor)
            upcomingEventsSection = EventsSection(
                mutableListOf(),
                "UPCOMING",
                upcomingHeaderColor,
                true,
                this,
                it
            )
            eventsListAdapter = SectionedRecyclerViewAdapter().apply {
                addSection(ongoingEventsSection)
                addSection(upcomingEventsSection)
            }
        }

        viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        viewModel.init()
        viewModel.ongoingEventsLiveData.observe(this, Observer { updateOngoingEventsList(it) })
        viewModel.upcomingEventsLiveData.observe(this, Observer { updateUpcomingEventsList(it) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        view.eventsList.apply {
            layoutManager = LinearLayoutManager(container?.context)
            adapter = eventsListAdapter
        }

        view.refreshLayout.setRefreshView(CustomRefreshView(context), ViewGroup.LayoutParams(refreshIconSize, refreshIconSize))
        view.refreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        isActive = true
        countDownManager.start()
    }

    override fun onPause() {
        super.onPause()
        isActive = false
        countDownManager.onPause()
    }

    override fun onResume() {
        super.onResume()
        isActive = true
        countDownManager.onResume()
    }

    override fun onStop() {
        super.onStop()
        isActive = false
    }

    private fun updateOngoingEventsList(events: List<Event>?) {
        events?.let {
            ongoingEventsSection.updateEventsList(it)
            eventsListAdapter.notifyDataSetChanged()
        }
        refreshLayout.setRefreshing(false)
    }

    private fun updateUpcomingEventsList(events: List<Event>?) {
        events?.let {
            val actualEvents = filterNextNUpcomingEvents(it, numberOfUpcomingEvents)
            upcomingEventsSection.updateEventsList(actualEvents)
            eventsListAdapter.notifyDataSetChanged()
        }
        refreshLayout.setRefreshing(false)
    }

    private fun filterNextNUpcomingEvents(events: List<Event>, n: Int): List<Event> {
        val actualEvents = mutableListOf<Event>()
        var numLeft = n
        events.forEach {
            if (numLeft > 0) {
                actualEvents.add(it)
                numLeft--
            } else {
                if (actualEvents.isNotEmpty() && actualEvents.last().startTime == it.startTime) {
                    actualEvents.add(it)
                }
            }
        }
        return actualEvents
    }

    override fun updateTime(timeUntil: Long) {
        val timeInfo = TimeInfo(timeUntil)

        if (isActive) {
            daysValue.setText(padNumber(timeInfo.days))
            hoursValue.setText(padNumber(timeInfo.hours))
            minutesValue.setText(padNumber(timeInfo.minutes))
        }
    }

    override fun updateTitle(newTitle: String) {
        if (isActive) {
            countdownTextView.text = newTitle
        }
    }

    override fun openEventInfoActivity(event: Event) {
        val eventInfoFragment = EventInfoFragment.newInstance(event.id)
        (activity as MainActivity?)?.switchFragment(eventInfoFragment, true)
    }

    private fun padNumber(number: Long) = String.format("%02d", number)
}