package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicalquiz.R
import com.example.musicalquiz.adapter.TrackListAdapter
import com.example.musicalquiz.databinding.FragmentPlaylistDetailsBinding
import com.example.musicalquiz.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch

class PlaylistDetailsFragment : Fragment() {
    private var _binding: FragmentPlaylistDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: PlaylistViewModel by viewModels()
    private val args: PlaylistDetailsFragmentArgs by navArgs()
    private lateinit var adapter: TrackListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadPlaylistTracks(args.playlistId)
        }
    }

    private fun setupRecyclerView() {
        adapter = TrackListAdapter(
            onTrackClick = { track ->
                val bundle = Bundle().apply {
                    putLong("itemId", track.id)
                    putBoolean("isTrack", true)
                }
                findNavController().navigate(
                    R.id.action_playlistDetailsFragment_to_navigation_details,
                    bundle
                )
            },
            onPreviewClick = { track ->
                // TODO: Implement preview functionality
            }
        )

        binding.tracksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PlaylistDetailsFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.currentPlaylistTracks.observe(viewLifecycleOwner) { tracks ->
                adapter.submitList(tracks)
                updateEmptyState(tracks.isEmpty())
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                // TODO: Show loading indicator if needed
        }
    }

    private fun loadPlaylistDetails() {
        // Now handled in onViewCreated with coroutine
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.tracksRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 