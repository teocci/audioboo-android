/**
 * This file is part of AudioBoo, an android program for audio blogging.
 * Copyright (C) 2009 BestBefore Media Ltd.
 * Copyright (C) 2010,2011 AudioBoo Ltd.
 * All rights reserved.
 *
 * Author: Jens Finkhaeuser <jens@finkhaeuser.de>
 *
 * $Id$
 **/

package fm.audioboo.application;

import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.Date;
import java.util.LinkedList;

import fm.audioboo.jni.FLACStreamEncoder;
import fm.audioboo.jni.FLACStreamDecoder;

import fm.audioboo.data.BooData;
import fm.audioboo.data.BooLocation;
import fm.audioboo.data.Tag;
import fm.audioboo.data.User;

import android.util.Log;

/**
 * Representation of a Boo's data.
 **/
public class Boo
{
  /***************************************************************************
   * Private constants
   **/
  // Log ID
  private static final String LTAG = "Boo";


  /***************************************************************************
   * Public constants
   **/
  // Serialized file extension.
  public static final String EXTENSION = ".boo";
  // Data dir extension
  public static final String DATA_EXTENSION = ".data";
  // Recording extension
  public static final String RECORDING_EXTENSION = ".rec";
  // Image file name
  public static final String IMAGE_FILE = "image.png";
  public static final String TEMP_IMAGE_FILE = "image.png";

  // Serialization UID
  public static final long serialVersionUID = 5505418760954089521L;



  /***************************************************************************
   * Public data
   **/
  public BooData                mData = null;


  /***************************************************************************
   * Implementation
   **/
  public Boo()
  {
    mData = new BooData();
  }



  public Boo(BooData data)
  {
    mData = data;
  }



  /**
   * Copies members from other to this Boo
   **/
  public void copyFrom(Boo other)
  {
    mData.mId = other.mData.mId;
    mData.mTitle = other.mData.mTitle;
    mData.mUUID = other.mData.mUUID;
    mData.mDuration = other.mData.mDuration;
    mData.mTags = other.mData.mTags;
    mData.mUser = other.mData.mUser;
    mData.mRecordedAt = other.mData.mRecordedAt;
    mData.mUpdatedAt = other.mData.mUpdatedAt;
    mData.mUploadedAt = other.mData.mUploadedAt;
    mData.mLocation = other.mData.mLocation;
    mData.mHighMP3Url = other.mData.mHighMP3Url;
    mData.mImageUrl = other.mData.mImageUrl;
    mData.mDetailUrl = other.mData.mDetailUrl;
    mData.mFilename = other.mData.mFilename;
    mData.mRecordings = other.mData.mRecordings;
    mData.mPlays = other.mData.mPlays;
    mData.mComments = other.mData.mComments;
  }



  /**
   * Tries to construct a Boo from a file with the given filename.
   **/
  public static Boo constructFromFile(String filename)
  {
    File f = new File(filename);
    if (!f.exists()) {
      return null;
    }


    try {
      ObjectInputStream is = new ObjectInputStream(new FileInputStream(f));
      BooData boo = (BooData) is.readObject();
      is.close();

      if (null == boo.mUpdatedAt) {
        boo.mUpdatedAt = new Date(f.lastModified());
      }
      if (null == boo.mFilename) {
        boo.mFilename = filename;
      }
      if (null == boo.mRecordings && null != boo.mHighMP3Url && boo.mHighMP3Url.getScheme().equals("file")) {
        Log.d(LTAG, "Purging old recording.");
        new File(boo.mHighMP3Url.getPath()).delete();
        return null;
      }

      return new Boo(boo);
    } catch (FileNotFoundException ex) {
      Log.e(LTAG, "File not found: " + filename);
    } catch (ClassNotFoundException ex) {
      Log.e(LTAG, "Class not found: " + filename);
    } catch (IOException ex) {
      Log.e(LTAG, "Error reading file: " + filename);
    }

    return null;
  }



  /**
   * Reloads a Boo from mFilename.
   **/
  public boolean reload()
  {
    Boo b = constructFromFile(mData.mFilename);
    if (null == b) {
      return false;
    }
    copyFrom(b);
    return true;
  }



  /**
   * Serializes the Boo class to a file specified by the given filename.
   **/
  public void writeToFile()
  {
    if (null == mData.mFilename) {
      throw new IllegalStateException("No filename set when attempting to save Boo.");
    }
    writeToFile(mData.mFilename);
  }



  public void writeToFile(String filename)
  {
    mData.mUpdatedAt = new Date();

    try {
      ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(new File(filename)));
      os.writeObject(mData);
      os.flush();
      os = null;
    } catch (FileNotFoundException ex) {
      Log.e(LTAG, "File not found: " + filename);
    } catch (IOException ex) {
      Log.e(LTAG, "Error writing file '" + filename + "': " + ex.getMessage());
    }
  }



  /**
   * Deletes the Boo file and it's data files.
   **/
  public void delete()
  {
    Globals.get().getBooManager().deleteBoo(this);
  }



  /**
   * @see BooData.getDuration
   **/
  public double getDuration()
  {
    if (null == mData) {
      return 0;
    }
    return mData.getDuration();
  }



  /**
   * Returns a new Recording metadata object for use in recording Boos. If the
   * latest known Recording is empty (does not have a duration), that is
   * returned, otherwise a new Recording is created and registered.
   **/
  public BooData.Recording getLastEmptyRecording()
  {
    if (null == mData.mRecordings) {
      mData.mRecordings = new LinkedList<BooData.Recording>();
    }

    BooData.Recording rec = null;
    if (0 != mData.mRecordings.size()) {
      BooData.Recording r = mData.mRecordings.get(mData.mRecordings.size() - 1);
      if (0 == r.mDuration) {
        rec = r;
      }
    }

    if (null == rec) {
      rec = new BooData.Recording(
          Globals.get().getBooManager().getNewRecordingFilename(this));
      mData.mRecordings.add(rec);
    }

    return rec;
  }




  public String toString()
  {
    if (null == mData) {
      return null;
    }
    return mData.toString();
  }



  public String getImageFilename()
  {
    return Globals.get().getBooManager().getImageFilename(this);
  }



  public String getTempImageFilename()
  {
    return Globals.get().getBooManager().getTempImageFilename(this);
  }



  public boolean isLocal()
  {
    return (mData != null && mData.mRecordings != null);
  }



  // Flattens the list of audio files as returned by BooManager.getAudioFiles()
  // into a single flac file. XXX Warning, this function blocks.
  public void flattenAudio()
  {
    // First, check if audio is already flattened. If that's the case, we don't
    // want to flatten everything again.
    if (null != mData.mHighMP3Url) {
      String filename = mData.mHighMP3Url.getPath();
      File high_f = new File(filename);
      if (!high_f.exists()) {
        mData.mHighMP3Url = null;
      }
      else {
        long latest = 0;
        for (BooData.Recording rec : mData.mRecordings) {
          File f = new File(rec.mFilename);
          long d = f.lastModified();
          if (d > latest) {
            latest = d;
          }
        }

        if (high_f.lastModified() > latest) {
          // This boo seems to be flattened already.
          return;
        }
        else {
          // This boo was flattened, but new recordings were made
          // afterwards. Delete the previously flattened file.
          high_f.delete();
          mData.mHighMP3Url = null;
        }
      }
    }

    // If we reached here, then we need to flatten the Boo again.
    String target = Globals.get().getBooManager().getNewRecordingFilename(this);

    // Flatten the audio files.
    FLACStreamEncoder encoder = null;

    for (BooData.Recording rec : mData.mRecordings) {
      //Log.d(LTAG, "Using recording: " + rec);
      FLACStreamDecoder decoder = new FLACStreamDecoder(rec.mFilename);

      int bufsize = decoder.minBufferSize();
      //Log.d(LTAG, "bufsize is: " + bufsize);
      ByteBuffer buffer = ByteBuffer.allocateDirect(bufsize);

      while (true) {
        int read = decoder.read(buffer, bufsize);
        if (read <= 0) {
          break;
        }
        //Log.d(LTAG, "read: " + read);


        if (null == encoder) {
          // Assume that all recordings share the format of the first recording.
          encoder = new FLACStreamEncoder(target, decoder.sampleRate(),
              decoder.channels(), decoder.bitsPerSample());
        }

        encoder.write(buffer, read);
      }

      encoder.flush();
      decoder.release();
      decoder = null;
    }

    if (null != encoder) {
      encoder.release();
    }
    encoder = null;

    // Next, set the high mp3 Uri for the Boo to be the target path.
    mData.mHighMP3Url = Uri.parse(String.format("file://%s", target));
    //Log.d(LTAG, "Flattened to: " + mHighMP3Url);

    // Right, persist this flattened URL
    writeToFile();
  }
}