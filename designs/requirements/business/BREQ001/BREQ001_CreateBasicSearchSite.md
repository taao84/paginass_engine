# BREQ001 Create basic search site

## GOALS
Create a very simple web site were people can for business, products, services or others by entering a few search terms such as business names, product names, service names or even people's names.

## ACCESS REQUIREMENTS AND GRANTS
This search option does not require users to be logged and its access should be free to anyone with the url.

## UI REQUIREMENT
The UI for this feature should be very simple. In the main page a simple text box in the middle of the screen were search terms can be entered and a button to send the request. Requests can also be sent by entering the search terms in the search text box and hitting the Enter key.

The search result should be a simple vertical list of the found data matching the entered search terms. The list should be sorted from the most relevant to least based on a score goven to each result. 

## TECHNICAL REQUIREMENT
Given some search terms a search should be conducted in the site's database trying to match the search terms with existing business business descriptions, business tags, keywords, and contact information.

The search should assign a score to each potential match. A higher score will indicate a higher match between the entered search terms and the data in the database. The result needs be sorted based on that score from higher to lower scores. 

The search result needs to be paginated returning 10 results per page to avoid overloading the system.
