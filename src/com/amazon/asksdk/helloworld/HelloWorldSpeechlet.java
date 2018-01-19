/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.amazon.asksdk.helloworld;

import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.speechlet.dialog.directives.DelegateDirective;
import com.amazon.speech.speechlet.dialog.directives.DialogDirective;
import com.amazon.speech.speechlet.dialog.directives.DialogIntent;
import com.amazon.speech.speechlet.dialog.directives.ElicitSlotDirective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.OutputSpeech;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This sample shows how to create a simple speechlet for handling speechlet requests.
 */
public class HelloWorldSpeechlet implements SpeechletV2 {
    private static final Logger log = LoggerFactory.getLogger(HelloWorldSpeechlet.class);

    private static final String att_PingingFor = "PingingFor";
    private static final String pingingFor_Meal = "Meal";
    private static final String pingingFor_MealConfirmation = "Meal Confirm";

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        log.info("onSessionStarted requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        log.info("OnSessionStarted");
        requestEnvelope.getSession().setAttribute("PingingFor", pingingFor_Meal);
        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        log.info("onLaunch requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        log.info("OnLaunch");

        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        Session currentSession = requestEnvelope.getSession();
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                requestEnvelope.getSession().getSessionId());
        log.info("OnIntent");

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        //return getOrder_MealResponse(intent, currentSession);
        switch (intentName)
        {
            case "AMAZON.HelpIntent": return getHelpResponse();
            case "test": if(allSlotsFilled(intent)){ return tell("true");}else {return tell("false");}
            case "Order_Meal": return getOrder_MealResponse(intent, currentSession);
            case "MealDialog": return launchDialog(requestEnvelope, "Order Complete with all data retrieved");
            default: return getAskResponse("HelloWorld", "This is unsupported.  Please try something else.");
        }
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        log.info("onSessionEnded requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        // any cleanup logic goes here
        log.info("OnSessionEnded");
    }

    private SpeechletResponse getTestResponse() {
        String speechText = "Test Successful";


        // Create the plain text output.
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);

        return SpeechletResponse.newTellResponse(speech);
    }

    private SpeechletResponse getOrder_MealResponse(Intent intent, Session session) {

        if(intent.getName().matches("MealDialog"))
        {
            return tell("Potato");
        }

        switch ((String) session.getAttribute("PingingFor"))
        {
            case pingingFor_Meal: return orderMeal(intent, session);
            case pingingFor_MealConfirmation: return confirmMeal(intent, session);

            default: return tell("Unknown PingingFor Value. Value received as: " + (String) session.getAttribute("PingingFor") );
        }
    }

    private SpeechletResponse orderMeal(Intent intent, Session session) {
        String speechText = "";
        if(intent.getName().matches("Order_Meal"))
        {
            if (intent.getSlot("meal").getValue() != null) {
                try {
                    speechText = "Ordering specific meal: " + intent.getSlot("meal").getValue() + " Is this correct?";// Create the plain text output.
                    session.setAttribute(att_PingingFor, pingingFor_MealConfirmation);
                    return ask(speechText, speechText);
                } catch (Exception e) {
                    speechText = "Error: " + e.toString();
                }

            } else {

                try {
                    speechText = "What would you like to order? "; //+ intent.getSlot("meal");
                    return ask(speechText, speechText);
                } catch (Exception e) {
                    speechText = "Error: " + e.toString();
                }
            }
            return tell("Error: Something went horribly wrong in orderMeal");
        }
        return tell("Error: in orderMeal Incorrect Intent: " + intent.getName());
    }

    private SpeechletResponse confirmMeal(Intent intent, Session session) {
        String speechText = "";
        if(intent.getName().matches("Yes"))
        {
            speechText = "Order Confirmed";// Create the plain text output.
            //session.setAttribute(att_PingingFor, pingingFor_MealConfirmation);
            return tell(speechText);

        }
        else if (intent.getName().matches("No"))
        {
            speechText = "Order Canceled. What would you like to order instead?";// Create the plain text output.
            session.setAttribute(att_PingingFor, pingingFor_Meal);
            return ask(speechText, speechText);
        }
        return tell("Error: in confirmMeal Incorrect Intent: " + intent.getName());
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getWelcomeResponse() {
        String speechText = "Welcome to the Alexa Skills Kit, you can say hello";
        return getAskResponse("HelloWorld", speechText);
    }

    /**
     * Creates a {@code SpeechletResponse} for the hello intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelloResponse() {
        String speechText = "Hello world";

        // Create the Simple card content.
        SimpleCard card = getSimpleCard("HelloWorld", speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);

        return SpeechletResponse.newTellResponse(speech, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the help intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelpResponse() {
        String speechText = "Placeholder Response to Help Intent!";
        return getAskResponse("HelloWorld", speechText);
    }

    /**
     * Helper method that creates a card object.
     * @param title title of the card
     * @param content body of the card
     * @return SimpleCard the display card to be sent along with the voice response.
     */
    private SimpleCard getSimpleCard(String title, String content) {
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(content);

        return card;
    }

    /**
     * Helper method for retrieving an OutputSpeech object when given a string of TTS.
     * @param speechText the text that should be spoken out to the user.
     * @return an instance of SpeechOutput.
     */
    private PlainTextOutputSpeech getPlainTextOutputSpeech(String speechText) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return speech;
    }

    /**
     * Helper method that returns a reprompt object. This is used in Ask responses where you want
     * the user to be able to respond to your speech.
     * @param outputSpeech The OutputSpeech object that will be said once and repeated if necessary.
     * @return Reprompt instance.
     */
    private Reprompt getReprompt(OutputSpeech outputSpeech) {
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);

        return reprompt;
    }

    /**
     * Helper method for retrieving an Ask response with a simple card and reprompt included.
     * @param cardTitle Title of the card that you want displayed.
     * @param speechText speech text that will be spoken to the user.
     * @return the resulting card and speech text.
     */
    private SpeechletResponse getAskResponse(String cardTitle, String speechText) {
        SimpleCard card = getSimpleCard(cardTitle, speechText);
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);
        Reprompt reprompt = getReprompt(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    private SpeechletResponse tell(String speechText)
    {
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);
        return SpeechletResponse.newTellResponse(speech);
    }

    private SpeechletResponse ask(String speechText, String promptText)
    {
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);
        PlainTextOutputSpeech promptOutput = getPlainTextOutputSpeech(promptText);
        Reprompt prompt = new Reprompt();
        prompt.setOutputSpeech(promptOutput);

        return SpeechletResponse.newAskResponse(speech, prompt);
    }

    private SpeechletResponse launchDialog(SpeechletRequestEnvelope<IntentRequest> requestEnvelope, String completeDialog) {
        IntentRequest request = requestEnvelope.getRequest();
        if (!request.getDialogState().toString().matches("COMPLETED"))
        {
            // Create updatedIntent and prefill information if needed
            DialogIntent updatedIntent = new DialogIntent(request.getIntent());
            DelegateDirective delegateDirective = new DelegateDirective();
            delegateDirective.setUpdatedIntent(updatedIntent);

            java.util.List<Directive> directives = new ArrayList<>();
            directives.add(delegateDirective);

            // Create SpeechletResponse
            SpeechletResponse response = new SpeechletResponse();
            response.setDirectives(directives);
            response.setNullableShouldEndSession(false);
            return response;
        }
        else
        {
            //slotAllFilled(); //Next Step logic
            if(requestEnvelope.getRequest().getIntent().getName().matches("MealDialog"))
            {
                switch (requestEnvelope.getRequest().getIntent().getSlot("Meal").getValue())
                {
                    case "meal 1":
                        if(requestEnvelope.getRequest().getIntent().getSlot("Allergy").getValue().equalsIgnoreCase("Mushrooms"))
                        {
                            return ask("Allergy Alert: Meal One contains mushrooms. Aborting Order.", "Allergy Alert: Meal One contains mushrooms. Aborting Order.");
                        }
                        return tell("Order Confirmed. Ordering Meal One");
                    case "meal 2":
                        if(requestEnvelope.getRequest().getIntent().getSlot("Allergy").getValue().equalsIgnoreCase("Sesame Seeds"))
                        {
                            return ask("Allergy Alert: Meal Two contains Sesame Seeds. Aborting Order.", "Allergy Alert: Meal Two contains Sesame Seeds. Aborting Order.");
                        }
                        return tell("Order Confirmed. Ordering Meal Two");
                    case "meal 3":
                        if(requestEnvelope.getRequest().getIntent().getSlot("Allergy").getValue().equalsIgnoreCase("Mushrooms"))
                        {
                            return ask("Allergy Alert: Meal Three contains mushrooms. Aborting Order.", "Allergy Alert: Meal Three contains mushrooms. Aborting Order.");
                        }
                        return tell("Order Confirmed. Ordering Meal Three");
                    default:
                        return launchSlotDialog(requestEnvelope, "Meal","We do not currently serve " + requestEnvelope.getRequest().getIntent().getSlot("Meal").getValue() + ", please choose something else", completeDialog);
                }
            }
            return tell(completeDialog);
        }
    }

    private SpeechletResponse launchSlotDialog(SpeechletRequestEnvelope<IntentRequest> requestEnvelope, String slotToElicit, String promptSpeech, String dialogCompleteSpeech) {
        IntentRequest request = requestEnvelope.getRequest();
        if (!request.getDialogState().toString().matches("COMPLETED"))
        {
            // Create updatedIntent and prefill information if needed
            DialogIntent updatedIntent = new DialogIntent(request.getIntent());
            ElicitSlotDirective dialogDirective = new ElicitSlotDirective();
            dialogDirective.setUpdatedIntent(updatedIntent);
            dialogDirective.setSlotToElicit(slotToElicit);

            java.util.List<Directive> directives = new ArrayList<>();
            directives.add(dialogDirective);

            // Create SpeechletResponse
            SpeechletResponse response = new SpeechletResponse();
            response.setDirectives(directives);
            response.setNullableShouldEndSession(false);
            PlainTextOutputSpeech aPlainTextOutputSpeech = getPlainTextOutputSpeech(promptSpeech);
            response.setOutputSpeech(aPlainTextOutputSpeech);
            return response;
        }
        else
        {
            return tell(promptSpeech);
        }
    }

    //Returns true if NONE of the Slots in the intent are null. (Slots do not have to be required slots)
    private boolean allSlotsFilled(Intent anIntent)
    {
        if(anIntent.getSlots().size() < 1)
        {
            return true;
        }
        else
        {
            ArrayList<Slot> slots = new ArrayList<Slot>(anIntent.getSlots().values());

            for (Slot aSlot : slots) {
                if (aSlot.getValue() == null) {
                    return false;
                }
            }
            return true;
        }
    }
}
