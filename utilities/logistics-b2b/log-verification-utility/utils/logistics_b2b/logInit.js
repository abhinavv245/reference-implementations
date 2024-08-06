const _ = require("lodash");
const dao = require("../../dao/dao");
const constants = require("../constants");
const utils = require("../utils");

const checkInit = async (data, msgIdSet) => {
  let initObj = {};
  let onSearch = dao.getValue("onSearchObj");
  let items = onSearch?.providers?.[0]?.items || [];
  const validateBillingTimeStamp = () => {
    billingOrderTimestamp = data.message.order.billing.time.timestamp;
    contextOrderTimestamp = data.context.timestamp;
    return billingOrderTimestamp === contextOrderTimestamp;
  };
  try {
    console.log("Validating billing order timestamp");
    if (!validateBillingTimeStamp(data)) {
      initObj["billingOrderTimestampErr"] =
        "Billing order timestamp is not as expected";
    }
  } catch (e) {
    console.log("Error while validating billing order timestamp");
  }
  const validateFulfillmentStops = () => {
    const order = data.message.order;

    if (!order.fulfillments || order.fulfillments.length === 0) {
      return false;
    }

    for (const fulfillment of order.fulfillments) {
      if (fulfillment.stops && fulfillment.stops.length > 1) {
        const gpsSet = new Set();

        for (const stop of fulfillment.stops) {
          if (gpsSet.has(stop.location.gps)) {
            return false; // Duplicate GPS found
          }
          gpsSet.add(stop.location.gps);
        }
      } else {
        return false; // Each fulfillment must have at least two stops
      }
    }

    return true;
  };
  try {
    console.log("Validating fulfillment stops");
    if (!validateFulfillmentStops(data)) {
      initObj["fulfillmentStopsErr"] = "Fulfillment stops are not as expected";
    }
  } catch (e) {
    console.log("Error while validating fulfillment stops");
  }
  function validateItems(orderItems, referenceItems, initObj) {
    try {
      console.log("Validating order items");

      orderItems.forEach((orderItem) => {
        const matchingItem = referenceItems.find(
          (refItem) => refItem.id === orderItem.id
        );

        if (!matchingItem) {
          initObj[
            `itemNotFoundErr_${orderItem.id}`
          ] = `Item with id ${orderItem.id} not found in the /on_search items.`;
          return;
        }

        Object.keys(orderItem).forEach((key) => {
          if (key === "tags") {
            if (!_.isEqual(orderItem.tags, matchingItem.tags)) {
              initObj[
                `itemTagsMismatchErr_${orderItem.id}`
              ] = `Mismatch in tags for item ${orderItem.id}`;
            }
          } else if (
            typeof orderItem[key] === "object" &&
            orderItem[key] !== null
          ) {
            Object.keys(orderItem[key]).forEach((nestedKey) => {
              if (
                !_.isEqual(
                  orderItem[key][nestedKey],
                  matchingItem[key]?.[nestedKey]
                )
              ) {
                initObj[
                  `itemFieldMismatchErr_${orderItem.id}_${key}_${nestedKey}`
                ] = `Mismatch in ${key}.${nestedKey} for item ${orderItem.id}`;
              }
            });
          } else if (!_.isEqual(orderItem[key], matchingItem[key])) {
            initObj[
              `itemFieldMismatchErr_${orderItem.id}_${key}`
            ] = `Mismatch in ${key} for item ${orderItem.id}`;
          }
        });
      });
    } catch (e) {
      console.log("Error while validating order items", e);
      initObj["itemValidationError"] =
        "An error occurred while validating order items";
    }
  }
  validateItems(data.message.order.items, items, initObj);
  return initObj;
};

module.exports = checkInit;
