package com.example.vasilvestre.gregy

public class PictureEntry {
    public var timestamp: Long
    lateinit var image: String
    lateinit var annotations: Map<String, Float>

    constructor(timestamp: Long, image: String, annotations: Map<String, Float>) {
        this.timestamp = timestamp
        this.image = image
        this.annotations = annotations
    }

    companion object {

    }
}