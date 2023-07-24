/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.audio.components;

import org.praxislive.code.GenerateTemplate;

import org.praxislive.audio.code.AudioCodeDelegate;

// default imports
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import org.praxislive.core.*;
import org.praxislive.core.types.*;
import org.praxislive.code.userapi.*;
import static org.praxislive.code.userapi.Constants.*;
import org.jaudiolibs.pipes.*;
import org.jaudiolibs.pipes.units.*;
import org.praxislive.audio.code.userapi.*;
import static org.praxislive.audio.code.userapi.AudioConstants.*;

/**
 *
 */
@GenerateTemplate(AudioFXChorus.TEMPLATE_PATH)
public class AudioFXChorus extends AudioCodeDelegate {
    
    final static String TEMPLATE_PATH = "resources/fx_chorus.pxj";

    // PXJ-BEGIN:body
    
    @In(1) AudioIn in1;
    @In(2) AudioIn in2;
    @Out(1) AudioOut out1;
    @Out(2) AudioOut out2;
    
    @UGen Chorus ch1, ch2;
    
    @P(1) @Type.Number(min=0, max=40, skew=2)
    Property depth;
    @P(2) @Type.Number(min=0, max=15, skew=2)
    Property rate;
    @P(3) @Type.Number(min=0, max=1)
    Property feedback;
    
    @Override
    public void init() {
        depth.link(ch1::depth).link(ch2::depth);
        rate.link(ch1::rate).link(ch2::rate);
        feedback.link(ch1::feedback).link(ch2::feedback);
        link(in1, ch1, out1);
        link(in2, ch2, out2);
    }
    
    // PXJ-END:body
    
}
