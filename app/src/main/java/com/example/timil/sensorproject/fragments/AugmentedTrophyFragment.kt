package com.example.timil.sensorproject.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.timil.sensorproject.database.TrophyDB
import com.example.timil.sensorproject.entities.Trophy
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment
import com.google.ar.sceneform.ux.TransformableNode
import org.jetbrains.anko.doAsync
import java.util.concurrent.CompletableFuture

class AugmentedTrophyFragment: ArFragment() {

    //private var trophyRenderable: ModelRenderable? = null
    //private var name: String? = null
    //private val DOUBLE_CLICK_TIME_DELTA: Long = 300// milliseconds
    private var renderable: ModelRenderable? = null
    private var arFragment: ArFragment? = null
    private var renderableFuture: CompletableFuture<ModelRenderable>? = null
    private val trophyUri = Uri.parse("trophyobjectfile.sfb")
    private var screenX: Int? = null
    private var screenY: Int? = null
    private var activityCallBack: AugmentedFragmentTrophyClickListener? = null
    private var bundle: Bundle? = null

    interface AugmentedFragmentTrophyClickListener {
        fun onARTrophyClick()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activityCallBack = context as AugmentedFragmentTrophyClickListener

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState:
    Bundle?): View? {

        bundle = this.arguments
        if(bundle != null){
            screenX = bundle?.getInt("x")
            screenY = bundle?.getInt("y")
        }

        val view = super.onCreateView(inflater, container, savedInstanceState)

        this.arSceneView.scene.addOnUpdateListener{ _ ->
            this.planeDiscoveryController.hide()
            add3dObject()
        }
        renderableFuture = ModelRenderable.builder()
                .setSource(context, trophyUri)
                .build()
        renderableFuture!!.thenAccept{ it -> renderable = it }

        return view
    }

    private fun add3dObject(){
        //Log.d("DBG", "add object click")

        val frame = this.arSceneView.arFrame
        val hits: List<HitResult>
        if (frame != null && renderable != null){
            hits = frame.hitTest(screenX!!.toFloat(), screenY!!.toFloat())
            for (hit in hits){
                val trackable = hit.trackable
                if (trackable is Plane){
                    val anchor = hit!!.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(this.arSceneView.scene)
                    val mNode = TransformableNode(this.transformationSystem)
                    mNode.setParent(anchorNode)
                    mNode.renderable = renderable
                    mNode.select()
                    renderable = null
                    mNode.setOnTapListener { _, _ ->
                        anchorNode.removeChild(mNode)
                        val db = TrophyDB.get(context!!)
                        doAsync {
                            db.trophyDao().delete(Trophy(bundle!!.getLong("id"), bundle!!.getDouble("latitude"), bundle!!.getDouble("longitude")))
                        }
                        activityCallBack!!.onARTrophyClick()
                        Log.d("DBG", "id: "+bundle?.getLong("id")+" lat: "+bundle?.getDouble("latitude")+ " long: "+bundle?.getDouble("longitude"))
                    }
                    break
                }
            }
        }
    }

}


